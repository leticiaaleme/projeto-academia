package br.com.gymtime.webcontroller;

import br.com.gymtime.dto.AlunoResponseDTO;
import br.com.gymtime.dto.ExercicioCreateDTO;
import br.com.gymtime.dto.TreinoCreateDTO;
import br.com.gymtime.dto.TreinoResponseDTO;
import br.com.gymtime.dto.TreinoUpdateDTO;
import br.com.gymtime.exception.ResourceNotFoundException;
import br.com.gymtime.service.AlunoService;
import br.com.gymtime.service.TreinoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web/alunos/{alunoId}/treinos")
public class TreinoWebController {

    private static final Logger logger = LoggerFactory.getLogger(TreinoWebController.class);

    private final TreinoService treinoService;
    private final AlunoService alunoService;

    @Autowired
    public TreinoWebController(TreinoService treinoService, AlunoService alunoService) {
        this.treinoService = treinoService;
        this.alunoService = alunoService;
    }

    /**
     * Método auxiliar para carregar os dados do aluno e adicioná-los ao Model.
     * Retorna um Optional do AlunoResponseDTO.
     * Se o aluno não for encontrado, adiciona uma mensagem de erro global ao model.
     */
    private Optional<AlunoResponseDTO> carregarAlunoParaModel(Long alunoId, Model model) {
        Optional<AlunoResponseDTO> alunoOpt = alunoService.getAlunoById(alunoId);
        if (alunoOpt.isEmpty()) {
            logger.warn("Tentativa de acessar treinos para aluno não encontrado com ID: {}", alunoId);
            model.addAttribute("errorMessageGlobal", "Aluno com ID " + alunoId + " não encontrado.");
            return Optional.empty();
        }
        model.addAttribute("aluno", alunoOpt.get()); // Adiciona o AlunoResponseDTO ao model
        return alunoOpt;
    }

    /**
     * Exibe a lista de treinos para um aluno específico.
     */
    @GetMapping
    public String listarTreinosDoAluno(@PathVariable Long alunoId, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Listando treinos para aluno ID: {}", alunoId);
        if (carregarAlunoParaModel(alunoId, model).isEmpty()) {
            // Se carregarAlunoParaModel já adiciona a mensagem, podemos apenas redirecionar
            // ou confiar que a view de destino (lista de alunos) mostrará o erro.
            // Para ser mais explícito com redirectAttributes:
            redirectAttributes.addFlashAttribute("errorMessage", model.getAttribute("errorMessageGlobal"));
            return "redirect:/web/alunos";
        }
        List<TreinoResponseDTO> treinos = treinoService.getTreinosByAlunoId(alunoId);
        model.addAttribute("treinos", treinos);
        return "treinos/lista-treinos"; // Path: templates/treinos/lista-treinos.html
    }

    /**
     * Mostra o formulário para criar um novo treino para um aluno.
     */
    @GetMapping("/novo")
    public String mostrarFormularioNovoTreino(@PathVariable Long alunoId, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Mostrando formulário de novo treino para aluno ID: {}", alunoId);
        Optional<AlunoResponseDTO> alunoOpt = carregarAlunoParaModel(alunoId, model);
        if (alunoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", model.getAttribute("errorMessageGlobal"));
            return "redirect:/web/alunos";
        }

        List<ExercicioCreateDTO> exerciciosIniciais = new ArrayList<>();
        // Opcional: Adicionar um campo de exercício em branco por padrão
        // exerciciosIniciais.add(new ExercicioCreateDTO("", ""));

        model.addAttribute("treinoForm", new TreinoCreateDTO("", "", alunoId, exerciciosIniciais));
        model.addAttribute("pageTitle", "Novo Treino para " + alunoOpt.get().nome());
        model.addAttribute("treinoId", null); // Indica que é um novo treino (para lógica no template)
        return "treinos/form-treino"; // Path: templates/treinos/form-treino.html
    }

    /**
     * Processa a submissão do formulário para criar um novo treino.
     */
    @PostMapping("/criar")
    public String criarTreino(@PathVariable Long alunoId,
                              @Valid @ModelAttribute("treinoForm") TreinoCreateDTO treinoCreateDTO,
                              BindingResult bindingResult, // Contém os resultados da validação
                              Model model, RedirectAttributes redirectAttributes) {

        logger.info("POST para /criar treino para alunoId: {}. Dados recebidos: {}", alunoId, treinoCreateDTO);

        Optional<AlunoResponseDTO> alunoOpt = carregarAlunoParaModel(alunoId, model);
        if (alunoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", model.getAttribute("errorMessageGlobal"));
            return "redirect:/web/alunos";
        }
        // Adiciona atributos necessários para renderizar o form novamente em caso de erro
        model.addAttribute("pageTitle", "Novo Treino para " + alunoOpt.get().nome());
        model.addAttribute("treinoId", null); // Garante que o template saiba que é criação

        // Validação adicional: o alunoId no DTO deve corresponder ao alunoId do path
        if (treinoCreateDTO.alunoId() == null || !treinoCreateDTO.alunoId().equals(alunoId)) {
            logger.warn("Inconsistência no ID do aluno. Path: {}, DTO alunoId: {}", alunoId, treinoCreateDTO.alunoId());
            // Adicionar um erro global ou específico ao campo se ele existir no DTO
            bindingResult.reject("alunoIdInvalido", "ID do aluno no formulário é inválido ou não corresponde ao da URL.");
        }

        // Se houver erros de validação (incluindo dos @NotBlank nos ExercicioCreateDTO)
        if (bindingResult.hasErrors()) {
            logger.warn("Erros de validação ao criar treino: {}", bindingResult.getAllErrors());
            // O objeto "treinoForm" (que é o treinoCreateDTO com os dados submetidos e erros)
            // já está no model por causa do @ModelAttribute. O Thymeleaf o usará para
            // repopular o formulário e exibir os erros.
            return "treinos/form-treino";
        }

        try {
            // O TreinoServiceImpl irá iterar sobre treinoCreateDTO.exercicios()
            logger.info("Chamando treinoService.createTreino com: {}", treinoCreateDTO);
            treinoService.createTreino(treinoCreateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Treino cadastrado com sucesso!");
            logger.info("Treino criado com sucesso para aluno ID: {}", alunoId);
        } catch (Exception e) {
            logger.error("Erro EXCEPCIONAL ao cadastrar treino para aluno ID: {}. Erro: {}", alunoId, e.getMessage(), e);
            model.addAttribute("errorMessageGlobal", "Erro ao cadastrar treino: " + e.getMessage());
            // "treinoForm" já está no model
            return "treinos/form-treino"; // Retorna ao formulário com a mensagem de erro global
        }
        return "redirect:/web/alunos/" + alunoId + "/treinos";
    }

    /**
     * Mostra o formulário para editar um treino existente.
     */
    @GetMapping("/editar/{treinoId}")
    public String mostrarFormularioEditarTreino(@PathVariable Long alunoId, @PathVariable Long treinoId, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Mostrando formulário de edição para treino ID: {} do aluno ID: {}", treinoId, alunoId);
        Optional<AlunoResponseDTO> alunoOpt = carregarAlunoParaModel(alunoId, model);
        if (alunoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", model.getAttribute("errorMessageGlobal"));
            return "redirect:/web/alunos";
        }

        Optional<TreinoResponseDTO> treinoOpt = treinoService.getTreinoByIdAndAlunoId(treinoId, alunoId);
        if (treinoOpt.isEmpty()) {
            logger.warn("Treino ID: {} não encontrado ou não pertence ao aluno ID: {}", treinoId, alunoId);
            redirectAttributes.addFlashAttribute("errorMessage", "Treino não encontrado ou não pertence a este aluno.");
            return "redirect:/web/alunos/" + alunoId + "/treinos";
        }

        TreinoResponseDTO treinoExistente = treinoOpt.get();
        List<ExercicioCreateDTO> exerciciosParaForm = Collections.emptyList();
        if(treinoExistente.exercicios() != null){
            exerciciosParaForm = treinoExistente.exercicios().stream()
                    .map(exResp -> new ExercicioCreateDTO(exResp.nomeExercicio(), exResp.seriesRepeticoes()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("treinoForm", new TreinoUpdateDTO(
                treinoExistente.nome(),
                treinoExistente.descricao(),
                exerciciosParaForm
        ));
        model.addAttribute("treinoId", treinoId); // Para a action e lógica do template
        model.addAttribute("pageTitle", "Editar Treino de " + alunoOpt.get().nome());
        return "treinos/form-treino";
    }

    /**
     * Processa a submissão do formulário para atualizar um treino existente.
     */
    @PostMapping("/atualizar/{treinoId}")
    public String atualizarTreino(@PathVariable Long alunoId, @PathVariable Long treinoId,
                                  @Valid @ModelAttribute("treinoForm") TreinoUpdateDTO treinoUpdateDTO,
                                  BindingResult bindingResult,
                                  Model model, RedirectAttributes redirectAttributes) {
        logger.info("POST para /atualizar treino ID: {} para alunoId: {}. Dados recebidos: {}", treinoId, alunoId, treinoUpdateDTO);

        Optional<AlunoResponseDTO> alunoOpt = carregarAlunoParaModel(alunoId, model);
        if (alunoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", model.getAttribute("errorMessageGlobal"));
            return "redirect:/web/alunos";
        }
        model.addAttribute("pageTitle", "Editar Treino de " + alunoOpt.get().nome());
        model.addAttribute("treinoId", treinoId); // Para repopular form em caso de erro

        // Validação adicional para garantir que o treino que está sendo editado realmente pertence ao aluno da URL
        Optional<TreinoResponseDTO> treinoExistenteOpt = treinoService.getTreinoByIdAndAlunoId(treinoId, alunoId);
        if (treinoExistenteOpt.isEmpty()) {
            logger.warn("Tentativa de atualizar treino ID: {} que não pertence ao aluno ID: {} ou não existe.", treinoId, alunoId);
            redirectAttributes.addFlashAttribute("errorMessage", "Operação inválida: Treino não encontrado ou não pertence a este aluno.");
            return "redirect:/web/alunos/" + alunoId + "/treinos";
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Erros de validação ao atualizar treino ID: {}: {}", treinoId, bindingResult.getAllErrors());
            // "treinoForm" (treinoUpdateDTO com os dados submetidos e erros) já está no model
            return "treinos/form-treino";
        }
        try {
            logger.info("Chamando treinoService.updateTreino para treino ID: {} com: {}", treinoId, treinoUpdateDTO);
            treinoService.updateTreino(treinoId, treinoUpdateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Treino atualizado com sucesso!");
            logger.info("Treino ID: {} atualizado com sucesso para aluno ID: {}", treinoId, alunoId);
        } catch (ResourceNotFoundException e) {
            logger.warn("ResourceNotFoundException ao atualizar treino ID: {}: {}", treinoId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/web/alunos/" + alunoId + "/treinos";
        } catch (Exception e) {
            logger.error("Erro EXCEPCIONAL ao atualizar treino ID: {} para aluno ID: {}", treinoId, alunoId, e);
            model.addAttribute("errorMessageGlobal", "Erro ao atualizar treino: " + e.getMessage());
            // "treinoForm" já está no model
            return "treinos/form-treino";
        }
        return "redirect:/web/alunos/" + alunoId + "/treinos";
    }

    /**
     * Deleta um treino específico de um aluno.
     */
    @GetMapping("/deletar/{treinoId}")
    public String deletarTreino(@PathVariable Long alunoId, @PathVariable Long treinoId,
                                RedirectAttributes redirectAttributes) {
        try {
            // Verificar se o treino pertence ao aluno antes de deletar
            Optional<TreinoResponseDTO> treino = treinoService.getTreinoByIdAndAlunoId(treinoId, alunoId);
            if (treino.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", // <-- CORRIGIDO
                        "Treino não encontrado ou não pertence a este aluno.");
                return "redirect:/web/alunos/" + alunoId + "/treinos";
            }

            treinoService.deleteTreino(treinoId);
            redirectAttributes.addFlashAttribute("successMessage", "Treino deletado com sucesso!"); // <-- CORRIGIDO
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", // <-- CORRIGIDO
                    "Erro ao deletar treino: " + e.getMessage());
        }
        return "redirect:/web/alunos/" + alunoId + "/treinos";
    }
}