package br.com.gymtime.service.impl;

import br.com.gymtime.dto.ExercicioCreateDTO;
import br.com.gymtime.dto.ExercicioResponseDTO;
import br.com.gymtime.dto.TreinoCreateDTO;
import br.com.gymtime.dto.TreinoResponseDTO;
import br.com.gymtime.dto.TreinoUpdateDTO;
import br.com.gymtime.exception.ResourceNotFoundException;
import br.com.gymtime.model.Aluno;
import br.com.gymtime.model.Exercicio;
import br.com.gymtime.model.Treino;
import br.com.gymtime.repository.AlunoRepository;
import br.com.gymtime.repository.TreinoRepository;
import br.com.gymtime.service.TreinoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TreinoServiceImpl implements TreinoService {

    private final TreinoRepository treinoRepository;
    private final AlunoRepository alunoRepository;

    @Autowired
    public TreinoServiceImpl(TreinoRepository treinoRepository, AlunoRepository alunoRepository) {
        this.treinoRepository = treinoRepository;
        this.alunoRepository = alunoRepository;
    }

    private ExercicioResponseDTO convertToExercicioResponseDTO(Exercicio exercicio) {
        if (exercicio == null) return null;
        return new ExercicioResponseDTO(
                exercicio.getId(),
                exercicio.getNomeExercicio(),
                exercicio.getSeriesRepeticoes()
        );
    }

    private TreinoResponseDTO convertToTreinoResponseDTO(Treino treino) {
        if (treino == null) return null;

        List<ExercicioResponseDTO> exercicioDTOs = Collections.emptyList();
        if (treino.getExercicios() != null) {
            exercicioDTOs = treino.getExercicios().stream()
                    .map(this::convertToExercicioResponseDTO)
                    .collect(Collectors.toList());
        }

        return new TreinoResponseDTO(
                treino.getId(),
                treino.getNome(),
                treino.getDescricao(),
                treino.getDataCriacao(),
                treino.getDataAtualizacao(),
                treino.getAluno() != null ? treino.getAluno().getId() : null,
                exercicioDTOs
        );
    }

    @Transactional
    @Override
    public TreinoResponseDTO createTreino(TreinoCreateDTO treinoCreateDTO) {
        Aluno aluno = alunoRepository.findById(treinoCreateDTO.alunoId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado com ID: " + treinoCreateDTO.alunoId() + " para associar ao treino."));

        Treino treino = new Treino();
        treino.setNome(treinoCreateDTO.nome());
        treino.setDescricao(treinoCreateDTO.descricao());
        treino.setAluno(aluno);

        if (treinoCreateDTO.exercicios() != null) {
            for (ExercicioCreateDTO exDTO : treinoCreateDTO.exercicios()) {
                // Usar getters agora que ExercicioCreateDTO é uma classe
                if (exDTO.getNomeExercicio() != null && !exDTO.getNomeExercicio().isBlank()) {
                    Exercicio exercicio = new Exercicio(exDTO.getNomeExercicio(), exDTO.getSeriesRepeticoes());
                    treino.addExercicio(exercicio);
                }
            }
        }

        Treino savedTreino = treinoRepository.save(treino);
        return convertToTreinoResponseDTO(savedTreino);
    }

    @Transactional(readOnly = true)
    @Override
    public List<TreinoResponseDTO> getTreinosByAlunoId(Long alunoId) {
        if (!alunoRepository.existsById(alunoId)) {
            throw new ResourceNotFoundException("Aluno não encontrado com ID: " + alunoId);
        }
        return treinoRepository.findByAlunoId(alunoId).stream()
                .map(this::convertToTreinoResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<TreinoResponseDTO> getTreinoById(Long id) {
        return treinoRepository.findById(id)
                .map(this::convertToTreinoResponseDTO);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<TreinoResponseDTO> getTreinoByIdAndAlunoId(Long treinoId, Long alunoId) {
        // Primeiro, verifica se o aluno existe para evitar uma busca desnecessária de treino
        if (!alunoRepository.existsById(alunoId)) {
            throw new ResourceNotFoundException("Aluno não encontrado com ID: " + alunoId);
        }
        return treinoRepository.findById(treinoId)
                .filter(treino -> treino.getAluno() != null && treino.getAluno().getId().equals(alunoId))
                .map(this::convertToTreinoResponseDTO);
    }

    @Transactional
    @Override
    public TreinoResponseDTO updateTreino(Long id, TreinoUpdateDTO treinoUpdateDTO) {
        Treino treino = treinoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treino não encontrado com ID: " + id));

        if (treinoUpdateDTO.nome() != null) {
            treino.setNome(treinoUpdateDTO.nome());
        }
        if (treinoUpdateDTO.descricao() != null) {
            treino.setDescricao(treinoUpdateDTO.descricao());
        }

        treino.getExercicios().clear(); // Limpa os exercícios existentes (orphanRemoval=true cuidará da exclusão)
        if (treinoUpdateDTO.exercicios() != null) {
            for (ExercicioCreateDTO exDTO : treinoUpdateDTO.exercicios()) {
                // Usar getters agora que ExercicioCreateDTO é uma classe
                if (exDTO.getNomeExercicio() != null && !exDTO.getNomeExercicio().isBlank()) {
                    Exercicio exercicio = new Exercicio(exDTO.getNomeExercicio(), exDTO.getSeriesRepeticoes());
                    treino.addExercicio(exercicio);
                }
            }
        }

        Treino updatedTreino = treinoRepository.save(treino);
        return convertToTreinoResponseDTO(updatedTreino);
    }

    @Transactional
    @Override
    public void deleteTreino(Long id) {
        log.info("Iniciando deleção do treino com ID: {}", id);
        Treino treino = treinoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treino não encontrado com id: " + id));

        // Remover referências em exercícios se necessário
        if (treino.getExercicios() != null) {
            treino.getExercicios().clear();
        }

        treinoRepository.delete(treino);
        treinoRepository.flush(); // Força a sincronização com o banco
        log.info("Treino com ID: {} deletado com sucesso", id);
    }
}