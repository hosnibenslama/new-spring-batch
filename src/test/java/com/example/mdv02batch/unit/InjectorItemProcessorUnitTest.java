package com.example.mdv02batch.injector.unit;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.processor.InjectorItemProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class InjectorItemProcessorUnitTest {

    @InjectMocks
    private InjectorItemProcessor processor;

    @Test
    void shouldReturnSameInstanceWithoutModification() throws Exception {
        BusinessDataLine item = new BusinessDataLine(
                "CTR;123456;CLIENT_001;20240101;ACTIVE",
                1,
                0,
                "CTR",
                List.of("CTR", "123456", "CLIENT_001", "20240101", "ACTIVE")
        );

        BusinessDataLine result = processor.process(item);

        assertThat(result).isSameAs(item);
    }
}
