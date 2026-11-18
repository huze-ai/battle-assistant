package com.example.battle_assistant.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "BATTLE_TEST_MYSQL", matches = "1")
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ElfControllerTests {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createsAndRetrievesNormalizedData() throws Exception {
        mvc.perform(get("/api/elves")).andExpect(status().isOk()).andExpect(content().json("[]"));
        String elfPath = mvc.perform(post("/api/elves").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":" 测试精灵 ","types":[" 水 ","水"]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("测试精灵"))
                .andExpect(jsonPath("$.types.length()").value(1))
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(get(elfPath)).andExpect(status().isOk())
                .andExpect(jsonPath("$.types[0]").value("水"));
        mvc.perform(get("/api/elves")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsInvalidInputWithoutCreatingRecords() throws Exception {
        for (String body : new String[] {
                "{}", "{\"name\":\" \u3000\",\"types\":[\"水\"]}",
                "{\"name\":\"测试\",\"types\":[]}",
                "{\"name\":\"测试\",\"types\":[null]}",
                "{\"name\":\"测试\",\"types\":[\" \u3000\"]}"}) {
            mvc.perform(post("/api/elves").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
        mvc.perform(get("/api/elves")).andExpect(content().json("[]"));
    }

    @Test
    void missingRecordAndInvalidPathHaveAppropriateStatusCodes() throws Exception {
        mvc.perform(get("/api/elves/" + Long.MAX_VALUE)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("精灵资料不存在"));
        mvc.perform(get("/api/elves/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/elves").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesAndDeletesAnElf() throws Exception {
        String elfPath = mvc.perform(post("/api/elves").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试\",\"types\":[\"水\"]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        mvc.perform(put(elfPath).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"更新\",\"types\":[\"火\",\"冰\"]}"))
                .andExpect(status().isOk());
        mvc.perform(get(elfPath)).andExpect(jsonPath("$.types[1]").value("冰"));
        mvc.perform(delete(elfPath)).andExpect(status().isNoContent());
        mvc.perform(get(elfPath)).andExpect(status().isNotFound());
    }

    @Test
    void catalogCrudKeepsDifferentKindsSeparate() throws Exception {
        String skillPath = mvc.perform(post("/api/skills").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试技能\",\"description\":\"演示\",\"element\":\"水\",\"power\":50}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        mvc.perform(get(skillPath)).andExpect(jsonPath("$.details.power").value(50));
        mvc.perform(get(skillPath.replace("/skills/", "/items/"))).andExpect(status().isNotFound());
        mvc.perform(put(skillPath).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试技能\",\"description\":\"更新\",\"priority\":-1}"))
                .andExpect(status().isOk());
        mvc.perform(get(skillPath)).andExpect(jsonPath("$.details.priority").value(-1));
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试树果\",\"description\":\"演示\",\"category\":\"树果\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/items")).andExpect(jsonPath("$[0].details.category").value("树果"));
        mvc.perform(post("/api/traits").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试特性\",\"description\":\"演示\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/traits")).andExpect(jsonPath("$[0].kind").value("traits"));
        mvc.perform(delete(skillPath)).andExpect(status().isNoContent());
        mvc.perform(get(skillPath)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidCatalogFields() throws Exception {
        mvc.perform(post("/api/skills").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试\",\"description\":\"演示\",\"accuracy\":101}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试\",\"description\":\"演示\",\"power\":50}"))
                .andExpect(status().isBadRequest());
    }
}
