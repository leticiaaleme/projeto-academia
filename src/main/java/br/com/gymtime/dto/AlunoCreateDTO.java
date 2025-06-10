package br.com.gymtime.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlunoCreateDTO(
        @NotBlank(message = "O nome não pode estar em branco.")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
        @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ ]+$", message = "O nome deve conter apenas letras e espaços.")
        String nome,

        @Email(message = "Formato de email inválido.")
        @NotBlank(message = "O email não pode estar em branco.")
        String email,

        // Espera apenas dígitos (10 ou 11). A máscara será aplicada no frontend.
        @Pattern(regexp = "^[0-9]{10,11}$|^$", message = "Telefone deve conter 10 ou 11 números, ou ser deixado em branco.")
        String telefone,

        // Espera apenas os 11 dígitos do CPF.
        @NotBlank(message = "O CPF não pode estar em branco.")
        @Pattern(regexp = "^[0-9]{11}$", message = "CPF deve conter exatamente 11 números.")
        String cpf
) {
}