// Aplica a máscara apropriada aos campos CPF e Telefone quando a página carrega,
// caso eles já possuam um valor (útil na tela de edição).
document.addEventListener('DOMContentLoaded', function() {
    const cpfInput = document.getElementById('cpf');
    if (cpfInput && cpfInput.value) {
        maskCpf({ target: cpfInput });
    }

    const telefoneInput = document.getElementById('telefone');
    if (telefoneInput && telefoneInput.value) {
        maskTelefone({ target: telefoneInput });
    }
});

/**
 * Aplica a máscara de CPF (XXX.XXX.XXX-XX) enquanto o usuário digita.
 * @param {Event} event O evento de input.
 */
function maskCpf(event) {
    let input = event.target;
    let value = input.value.replace(/\D/g, ''); // Remove tudo que não é dígito
    value = value.substring(0, 11); // Limita a 11 dígitos

    let maskedValue = '';
    if (value.length > 9) {
        maskedValue = value.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
    } else if (value.length > 6) {
        maskedValue = value.replace(/(\d{3})(\d{3})(\d{1,3})/, '$1.$2.$3');
    } else if (value.length > 3) {
        maskedValue = value.replace(/(\d{3})(\d{1,3})/, '$1.$2');
    } else {
        maskedValue = value;
    }
    input.value = maskedValue;
}

/**
 * Aplica a máscara de telefone (XX) XXXXX-XXXX enquanto o usuário digita.
 * @param {Event} event O evento de input.
 */
function maskTelefone(event) {
    let input = event.target;
    let value = input.value.replace(/\D/g, ''); // Remove tudo que não é dígito
    value = value.substring(0, 11); // Limita a 11 dígitos (DDD + 9 dígitos)

    let maskedValue = '';
    if (value.length > 10) { // Celular (XX) XXXXX-XXXX
        maskedValue = value.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
    } else if (value.length > 6) { // Fixo com 8 dígitos ou Celular incompleto
        maskedValue = value.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3');
    } else if (value.length > 2) { // DDD + início do número
        maskedValue = value.replace(/(\d{2})(\d*)/, '($1) $2');
    } else if (value.length > 0) { // Início do DDD
        maskedValue = value.replace(/(\d*)/, '($1');
    } else {
        maskedValue = ''; // Limpa se estiver vazio
    }
    input.value = maskedValue;
}

/**
 * Remove as máscaras dos campos CPF e Telefone antes do formulário ser submetido,
 * garantindo que apenas os dígitos sejam enviados ao backend.
 * @param {HTMLFormElement} form O formulário que está sendo submetido.
 */
function unmaskFields(form) {
    const cpfInput = form.querySelector('#cpf');
    if (cpfInput) {
        cpfInput.value = cpfInput.value.replace(/\D/g, '');
    }
    const telefoneInput = form.querySelector('#telefone');
    if (telefoneInput) {
        telefoneInput.value = telefoneInput.value.replace(/\D/g, '');
    }
}