package br.ufal.ic.p2.wepayu.models;

/**
 * Destino dos contracheques organizados por seção da folha. Cada tipo de
 * empregado escolhe sua seção por polimorfismo, sem que quem monta a
 * folha precise conhecer o tipo concreto.
 */
public interface SecoesFolha {

    void adicionarHorista(Contracheque contracheque);

    void adicionarAssalariado(Contracheque contracheque);

    void adicionarComissionado(Contracheque contracheque);
}
