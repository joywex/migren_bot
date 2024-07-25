package com.example.migren.migrbot.States;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class UserQuestionState {

    private String lastQuestion;
}
