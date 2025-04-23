package com.example.apigateway.controller;

import com.example.apigateway.dto.TaskDTO;
import com.example.apigateway.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class WebController {

    private final WebClient webClient;

    @Autowired
    public WebController(WebClient webClient) {
        this.webClient = webClient;
    }

    @PostMapping("/auth/register")
    public Mono<UserDTO> registerUser(@RequestBody UserDTO user) {
        return webClient.post()
                .uri("lb://user-service/auth/register")
                .bodyValue(user)
                .retrieve()
                .bodyToMono(UserDTO.class);
    }

    @GetMapping("/tasks")
    public Flux<TaskDTO> getTasks(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return webClient.get()
                .uri("lb://task-service/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToFlux(TaskDTO.class);
    }

    @PostMapping("/tasks")
    public Mono<TaskDTO> createTask(@RequestBody TaskDTO task, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return webClient.post()
                .uri("lb://task-service/tasks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(task)
                .retrieve()
                .bodyToMono(TaskDTO.class);
    }

    @DeleteMapping("/tasks/{id}")
    public Mono<Void> deleteTask(@PathVariable Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return webClient.delete()
                .uri("lb://task-service/tasks/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
