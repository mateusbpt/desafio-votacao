package com.br.vote.service;

import com.br.vote.client.ApiValidationClient;
import com.br.vote.domain.Associate;
import com.br.vote.domain.Session;
import com.br.vote.domain.Vote;
import com.br.vote.domain.requests.VoteRequest;
import com.br.vote.exception.AssociateNoExistsException;
import com.br.vote.exception.AssociateUnableToVoteException;
import com.br.vote.exception.AssociateVotedException;
import com.br.vote.exception.FinishedSessionException;
import com.br.vote.exception.SessionNoExistsException;
import com.br.vote.mapper.VoteMapper;
import com.br.vote.repository.AssociateRepository;
import com.br.vote.repository.SessionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateVoteService {

    private final SessionRepository sessionRepository;
    private final AssociateRepository associateRepository;
    private final ApiValidationClient apiValidationClient;

    public Mono<Void> run(String sessionId, String associateId, VoteRequest request) {
        var vote = VoteMapper.toVote(request);
        return associateRepository
                .findById(associateId)
                .switchIfEmpty(Mono.error(new AssociateNoExistsException()))
                .flatMap(associate -> addAssociateAndApiValidation(vote, associate))
                .flatMap(ableToVote -> validateAssociateAbleToVoteAndFindSession(ableToVote, sessionId))
                .switchIfEmpty(Mono.error(new SessionNoExistsException()))
                .flatMap(session -> this.validateAndCreateVote(vote, session))
                .then()
                .doOnNext(
                        v -> log.info("Voto adicionado com sucesso. session={}, associate={}", sessionId, associateId));
    }

    private Mono<Boolean> addAssociateAndApiValidation(Vote vote, Associate associate) {
        vote.setAssociate(associate);
        return apiValidationClient.associateAbleToVote(associate.getDocument());
    }

    public Mono<Session> validateAssociateAbleToVoteAndFindSession(Boolean ableToVote, String sessionId) {
        if (!ableToVote) {
            return Mono.error(new AssociateUnableToVoteException());
        }
        return sessionRepository.findById(sessionId);
    }

    private Mono<Session> validateAndCreateVote(Vote vote, Session session) {
        var votes = session.getVotes();
        var now = LocalDateTime.now();
        var finishedSession = session.getEndDate().isBefore(now);
        var checkAssociateVote = votes.stream().anyMatch(v -> v.getAssociate()
                .getDocument()
                .equals(vote.getAssociate().getDocument()));

        if (finishedSession) {
            log.info("Erro ao adicionar voto: sessao={} finalizada.", session.getId());
            return Mono.error(new FinishedSessionException());
        }

        if (checkAssociateVote) {
            log.info(
                    "Erro ao adicionar voto: associado={} ja realizou o voto.",
                    vote.getAssociate().getId());
            return Mono.error(new AssociateVotedException());
        }

        session.getVotes().add(vote);
        return sessionRepository.save(session);
    }
}
