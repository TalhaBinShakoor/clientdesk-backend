package com.clientdesk.comment;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> findAll(
            @RequestParam(required = false) UUID workRequestId,
            @RequestParam(required = false) UUID projectTaskId
    ) {
        return commentService.findAll(workRequestId, projectTaskId);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public CommentResponse create(@Valid @RequestBody CommentCreateRequest request) {
        return commentService.create(request);
    }
}
