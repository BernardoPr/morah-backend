package org.morah.morah.portaria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.portaria.dto.VisitanteInput;
import org.morah.morah.portaria.modelo.Visitante;
import org.morah.morah.portaria.repositorio.VisitanteRepository;
import org.morah.morah.portaria.servico.VisitanteService;

/** Cadastro de visitantes: reaproveitamento pelo documento em vez de duplicar. */
@ExtendWith(MockitoExtension.class)
class VisitanteServiceTest {

    @Mock
    private VisitanteRepository visitanteRepository;

    private VisitanteService service;

    @BeforeEach
    void montarService() {
        service = new VisitanteService(visitanteRepository);
    }

    private void salvarDevolveOProprioObjeto() {
        when(visitanteRepository.save(any(Visitante.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Test
    @DisplayName("mesmo documento (com ou sem pontuacao) reaproveita o cadastro e atualiza nome/telefone")
    void reaproveitaPeloDocumento() {
        Visitante existente = PortariaFixtures.visitante(10L, "Joao");
        existente.setTelefone("11911110000");
        when(visitanteRepository.findFirstByCondominioIdAndDocumento(PortariaFixtures.CONDOMINIO, "123456789"))
                .thenReturn(Optional.of(existente));
        salvarDevolveOProprioObjeto();

        Visitante visitante = service.registrar(PortariaFixtures.CONDOMINIO,
                new VisitanteInput(" Joao da Silva ", "12.345.678-9", "11922220000"));

        assertThat(visitante).isSameAs(existente);
        assertThat(visitante.getId()).isEqualTo(10L);
        assertThat(visitante.getNome()).isEqualTo("Joao da Silva");
        assertThat(visitante.getTelefone()).isEqualTo("11922220000");
    }

    @Test
    @DisplayName("reaproveitar sem telefone nao apaga o telefone ja conhecido")
    void naoApagaTelefone() {
        Visitante existente = PortariaFixtures.visitante(10L, "Joao");
        existente.setTelefone("11911110000");
        when(visitanteRepository.findFirstByCondominioIdAndDocumento(PortariaFixtures.CONDOMINIO, "123456789"))
                .thenReturn(Optional.of(existente));
        salvarDevolveOProprioObjeto();

        Visitante visitante = service.registrar(PortariaFixtures.CONDOMINIO, new VisitanteInput("Joao", "123456789", null));

        assertThat(visitante.getTelefone()).isEqualTo("11911110000");
    }

    @Test
    @DisplayName("documento novo cria um cadastro no condominio, com o documento normalizado")
    void criaQuandoDocumentoNovo() {
        when(visitanteRepository.findFirstByCondominioIdAndDocumento(PortariaFixtures.CONDOMINIO, "MG1234567"))
                .thenReturn(Optional.empty());
        salvarDevolveOProprioObjeto();

        Visitante visitante = service.registrar(PortariaFixtures.CONDOMINIO, new VisitanteInput("Maria", "mg-1.234.567", null));

        assertThat(visitante.getId()).isNull();
        assertThat(visitante.getCondominioId()).isEqualTo(PortariaFixtures.CONDOMINIO);
        assertThat(visitante.getDocumento()).isEqualTo("MG1234567");
    }

    @Test
    @DisplayName("sem documento nao ha como reconhecer a pessoa: sempre cria")
    void semDocumentoSempreCria() {
        salvarDevolveOProprioObjeto();

        Visitante visitante = service.registrar(PortariaFixtures.CONDOMINIO, new VisitanteInput("Entregador", " - ", null));

        assertThat(visitante.getDocumento()).isNull();
        verify(visitanteRepository, never()).findFirstByCondominioIdAndDocumento(anyLong(), anyString());
    }
}
