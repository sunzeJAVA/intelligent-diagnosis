package com.company.intelligentdiagnosis.control.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void shouldCreateSuccessResult() {
        Result<String, String> result = Result.success("ok");

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(((Result.Success<String, String>) result).value()).isEqualTo("ok");
    }

    @Test
    void shouldCreateFailureResult() {
        Result<String, String> result = Result.failure("error");

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<String, String>) result).error()).isEqualTo("error");
    }
}
