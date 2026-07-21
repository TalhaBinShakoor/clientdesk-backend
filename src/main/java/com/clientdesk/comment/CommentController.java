package com.clientdesk.comment;

import com.clientdesk.api.ApiPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@Validated
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ApiPage<CommentResponse> findAll(
            @RequestParam(required = false) UUID workRequestId,
            @RequestParam(required = false) UUID projectTaskId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        return commentService.findAll(workRequestId, projectTaskId, page, size);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public CommentResponse create(@Valid @RequestBody CommentCreateRequest request) {
        return commentService.create(request);
    }
}
