package com.intentwise.ingestion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class GenericDataIngestionServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthAndSourcesAreAvailable() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));

		mockMvc.perform(get("/api/v1/sources"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='nasa-apod')]").exists())
				.andExpect(jsonPath("$[?(@.id=='rickandmorty-characters')]").exists());
	}
}
