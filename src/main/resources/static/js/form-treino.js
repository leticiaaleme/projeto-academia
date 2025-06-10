document.addEventListener('DOMContentLoaded', () => {
    // Configura a lógica para o formulário de criação
    const createContainer = document.getElementById('exerciciosContainerCreate');
    const createAddButton = document.getElementById('addExercicioBtnCreate');
    if (createContainer && createAddButton) {
        setupDynamicExerciseHandling(createContainer, createAddButton, '-create');
    }

    // Configura a lógica para o formulário de atualização
    const updateContainer = document.getElementById('exerciciosContainerUpdate');
    const updateAddButton = document.getElementById('addExercicioBtnUpdate');
    if (updateContainer && updateAddButton) {
        setupDynamicExerciseHandling(updateContainer, updateAddButton, '-update');
    }
});

/**
 * Configura os eventos de clique para adicionar e remover exercícios em um container.
 * @param {HTMLElement} container - O elemento que contém os itens de exercício.
 * @param {HTMLElement} addButton - O botão para adicionar novos exercícios.
 * @param {string} idSuffix - O sufixo para garantir IDs únicos ('-create' ou '-update').
 */
function setupDynamicExerciseHandling(container, addButton, idSuffix) {
    // Evento para ADICIONAR um novo exercício
    addButton.addEventListener('click', () => {
        const newIndex = container.querySelectorAll('.exercise-item').length;

        const newExercicioDiv = document.createElement('div');
        newExercicioDiv.classList.add('exercise-item');
        newExercicioDiv.innerHTML = `
            <label for="exercicios[${newIndex}].nomeExercicio${idSuffix}">Exercício:</label>
            <input type="text" id="exercicios[${newIndex}].nomeExercicio${idSuffix}" name="exercicios[${newIndex}].nomeExercicio" placeholder="Nome do Exercício"/>
            <div class="error-message"></div>

            <label for="exercicios[${newIndex}].seriesRepeticoes${idSuffix}">Séries/Rep:</label>
            <input type="text" id="exercicios[${newIndex}].seriesRepeticoes${idSuffix}" name="exercicios[${newIndex}].seriesRepeticoes" placeholder="Ex: 3x10"/>
            <div class="error-message"></div>

            <button type="button" class="remove-exercise-btn">Remover</button>
        `;
        container.appendChild(newExercicioDiv);
    });

    // Evento para REMOVER um exercício (usando delegação de eventos)
    container.addEventListener('click', (event) => {
        if (event.target.classList.contains('remove-exercise-btn')) {
            event.target.closest('.exercise-item').remove();
            reindexExercises(container, idSuffix);
        }
    });
}

/**
 * Reindexa os nomes e IDs dos campos de exercício para que sejam sequenciais (0, 1, 2, ...).
 * @param {HTMLElement} container - O container dos exercícios.
 * @param {string} idSuffix - O sufixo de ID.
 */
function reindexExercises(container, idSuffix) {
    const items = container.querySelectorAll('.exercise-item');
    items.forEach((item, index) => {
        // Re-index labels e inputs
        const nomeLabel = item.querySelector(`label[for*="nomeExercicio"]`);
        const nomeInput = item.querySelector(`input[id*="nomeExercicio"]`);
        const seriesLabel = item.querySelector(`label[for*="seriesRepeticoes"]`);
        const seriesInput = item.querySelector(`input[id*="seriesRepeticoes"]`);

        // CORREÇÃO APLICADA AQUI: Removidas as barras invertidas
        if (nomeLabel) nomeLabel.htmlFor = `exercicios[${index}].nomeExercicio${idSuffix}`;
        if (nomeInput) {
            nomeInput.id = `exercicios[${index}].nomeExercicio${idSuffix}`;
            nomeInput.name = `exercicios[${index}].nomeExercicio`;
        }

        if (seriesLabel) seriesLabel.htmlFor = `exercicios[${index}].seriesRepeticoes${idSuffix}`;
        if (seriesInput) {
            seriesInput.id = `exercicios[${index}].seriesRepeticoes${idSuffix}`;
            seriesInput.name = `exercicios[${index}].seriesRepeticoes`;
        }
    });
}