package com.example.taskservice.controller;

import com.example.taskservice.dto.TaskDTO;
import com.example.taskservice.dto.UserDTO;
import com.example.taskservice.model.Task;
import com.example.taskservice.service.TaskService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.jsonwebtoken.JwtParser;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private WebClient webClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Mono<Long> getUserId(String token) {
        return Mono.fromCallable(() -> {
                    JwtParser parser = Jwts.parser()
                            .setSigningKey(jwtSecret.getBytes())
                            .build();
                    Claims claims = parser.parseClaimsJws(token).getBody();
                    return claims.getSubject();
                })
                .flatMap(username -> webClient.get()
                        .uri("lb://user-service/users/" + username)
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError(), response ->
                                Mono.error(new RuntimeException("User not found")))
                        .bodyToMono(UserDTO.class))
                .map(UserDTO::getId)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .onErrorMap(e -> new RuntimeException("Invalid token or user not found", e));
    }

    private TaskDTO toTaskDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setCompleted(task.isCompleted());
        dto.setUserId(task.getUserId());
        return dto;
    }

    private Task toTask(TaskDTO dto) {
        Task task = new Task();
        task.setId(dto.getId());
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setCompleted(dto.isCompleted());
        task.setUserId(dto.getUserId());
        return task;
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<TaskDTO>>> getTasks(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return getUserId(token)
                .flatMap(userId -> Mono.just(taskService.getTasksByUserId(userId)))
                .map(tasks -> tasks.stream().map(this::toTaskDTO).collect(Collectors.toList()))
                .map(taskDtos -> ResponseEntity.ok(Flux.fromIterable(taskDtos)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<TaskDTO>> getTask(@PathVariable Long id) {
        return Mono.justOrEmpty(taskService.getTaskById(id))
                .map(this::toTaskDTO)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<ResponseEntity<TaskDTO>> createTask(@RequestBody TaskDTO taskDTO, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return getUserId(token)
                .map(userId -> {
                    Task task = toTask(taskDTO);
                    task.setUserId(userId);
                    return task;
                })
                .map(taskService::saveTask)
                .map(this::toTaskDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteTask(@PathVariable Long id) {
        return Mono.justOrEmpty(taskService.getTaskById(id))
                .doOnNext(task -> taskService.deleteTask(id))
                .map(task -> ResponseEntity.noContent().<Void>build())
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}
