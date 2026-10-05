package org.morah.morah.unidade.modelo;

/**
 * Regras de formato do CPF usadas pelo modulo Minha Unidade.
 *
 * <p>O app pode mandar o CPF com ou sem pontuacao ("123.456.789-01" ou "12345678901"),
 * mas no banco ele e sempre gravado so com os digitos - e assim que o login procura o
 * usuario ({@code AutenticacaoPorSenha}). Se cada classe normalizasse do seu jeito, a busca
 * de "pessoa ja cadastrada" falharia por causa de um ponto.
 */
public final class Cpf {

    /**
     * Formato aceito nos DTOs de entrada ({@code @Pattern}): 11 digitos, com ou sem a
     * pontuacao padrao. A string vazia tambem passa, para o CPF opcional da solicitacao.
     */
    public static final String FORMATO = "(\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2})?";

    public static final String MENSAGEM_FORMATO = "o CPF deve ter 11 digitos (com ou sem pontuacao)";

    private Cpf() {
    }

    /** Deixa so os digitos; devolve {@code null} para CPF ausente ou em branco. */
    public static String normalizar(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digitos = cpf.replaceAll("[^0-9]", "");
        return digitos.isEmpty() ? null : digitos;
    }
}
