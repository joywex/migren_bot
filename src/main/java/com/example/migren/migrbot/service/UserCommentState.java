package com.example.migren.migrbot.service;

import lombok.Data;
import org.springframework.stereotype.Component;


@Component
@Data
public class UserCommentState {

    private String currentState;
    private String comment;
}
