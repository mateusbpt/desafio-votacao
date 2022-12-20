package com.br.vote.controller;

import com.br.vote.domain.requests.SessionRequest;
import com.br.vote.domain.requests.VoteRequest;
import com.br.vote.service.CreateSessionService;
import com.br.vote.service.CreateVoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionController {

    private final CreateSessionService createSessionService;
    private final CreateVoteService createVoteService;

    @PostMapping("/agenda/{agendaId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> create(@PathVariable String agendaId, @Valid @RequestBody SessionRequest request) {
        return createSessionService.run(agendaId, request);
    }

    @PostMapping("/{sessionId}/associate/{associateId}")
    public Mono<Void> createVote(
            @PathVariable String sessionId, @PathVariable String associateId, @RequestBody VoteRequest request) {
        return createVoteService.run(sessionId, associateId, request);
    }
}
