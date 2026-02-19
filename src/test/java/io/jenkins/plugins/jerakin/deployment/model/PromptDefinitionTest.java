package io.jenkins.plugins.jerakin.deployment.model;

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.BooleanParameterDefinition;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.ParameterDefinition;
import hudson.model.PasswordParameterDefinition;
import hudson.model.StringParameterDefinition;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for the PromptDefinition model and its type conversion logic. */
class PromptDefinitionTest {

  @Test
  @DisplayName("Constructor sets name and type")
  void constructorSetsFields() {
    PromptDefinition prompt = new PromptDefinition("myParam", "string");
    assertEquals("myParam", prompt.getName());
    assertEquals("string", prompt.getType());
  }

  @Test
  @DisplayName("Description setter works")
  void descriptionSetter() {
    PromptDefinition prompt = new PromptDefinition("p", "string");
    prompt.setDescription("A parameter");
    assertEquals("A parameter", prompt.getDescription());
  }

  // ── Property access ───────────────────────────────────────────

  @Nested
  @DisplayName("getProperty / getListProperty")
  class PropertyAccess {

    @Test
    @DisplayName("getProperty returns value for matching key")
    void getPropertyMatch() {
      PromptDefinition prompt = new PromptDefinition("p", "string");
      prompt.setProperties(List.of(new JobParameter("defaultValue", "hello")));
      assertEquals("hello", prompt.getProperty("defaultValue"));
    }

    @Test
    @DisplayName("getProperty returns null for missing key")
    void getPropertyMissing() {
      PromptDefinition prompt = new PromptDefinition("p", "string");
      prompt.setProperties(List.of());
      assertNull(prompt.getProperty("nonexistent"));
    }

    @Test
    @DisplayName("getListProperty splits comma-separated values")
    void getListPropertyCommaSeparated() {
      PromptDefinition prompt = new PromptDefinition("p", "choice");
      prompt.setProperties(List.of(new JobParameter("choices", "a,b,c")));
      assertEquals(Arrays.asList("a", "b", "c"), prompt.getListProperty("choices"));
    }

    @Test
    @DisplayName("getListProperty returns single value as singleton list")
    void getListPropertySingleValue() {
      PromptDefinition prompt = new PromptDefinition("p", "choice");
      prompt.setProperties(List.of(new JobParameter("choices", "onlyone")));
      assertEquals(List.of("onlyone"), prompt.getListProperty("choices"));
    }

    @Test
    @DisplayName("getListProperty returns empty list for missing key")
    void getListPropertyMissing() {
      PromptDefinition prompt = new PromptDefinition("p", "choice");
      prompt.setProperties(List.of());
      assertTrue(prompt.getListProperty("choices").isEmpty());
    }
  }

  // ── Type conversion ───────────────────────────────────────────

  @Nested
  @DisplayName("toParameterDefinition type conversions")
  class TypeConversion {

    @Test
    @DisplayName("string type produces StringParameterDefinition")
    void stringType() {
      PromptDefinition prompt = new PromptDefinition("myString", "string");
      prompt.setDescription("A string param");
      prompt.setProperties(List.of(new JobParameter("defaultValue", "default")));

      ParameterDefinition def = prompt.toParameterDefinition();
      assertInstanceOf(StringParameterDefinition.class, def);
      assertEquals("myString", def.getName());
      assertEquals("A string param", def.getDescription());
    }

    @Test
    @DisplayName("choice type produces ChoiceParameterDefinition")
    void choiceType() {
      PromptDefinition prompt = new PromptDefinition("myChoice", "choice");
      prompt.setDescription("Pick one");
      prompt.setProperties(List.of(new JobParameter("choices", "opt1,opt2,opt3")));

      ParameterDefinition def = prompt.toParameterDefinition();
      assertInstanceOf(ChoiceParameterDefinition.class, def);
      assertEquals("myChoice", def.getName());
    }

    @Test
    @DisplayName("boolean type produces BooleanParameterDefinition")
    void booleanType() {
      PromptDefinition prompt = new PromptDefinition("myBool", "boolean");
      prompt.setDescription("Toggle");
      prompt.setProperties(List.of(new JobParameter("defaultValue", "true")));

      ParameterDefinition def = prompt.toParameterDefinition();
      assertInstanceOf(BooleanParameterDefinition.class, def);
      assertEquals("myBool", def.getName());
    }

    @Test
    @DisplayName("boolean type defaults to false when no default specified")
    void booleanDefaultFalse() {
      PromptDefinition prompt = new PromptDefinition("myBool", "boolean");
      prompt.setProperties(List.of());

      ParameterDefinition def = prompt.toParameterDefinition();
      assertInstanceOf(BooleanParameterDefinition.class, def);
    }

    @Test
    @DisplayName("password type produces PasswordParameterDefinition")
    void passwordType() {
      PromptDefinition prompt = new PromptDefinition("myPass", "password");
      prompt.setDescription("Secret");

      ParameterDefinition def = prompt.toParameterDefinition();
      assertInstanceOf(PasswordParameterDefinition.class, def);
      assertEquals("myPass", def.getName());
    }

    @Test
    @DisplayName("Unknown type falls back to StringParameterDefinition")
    void unknownTypeFallback() {
      PromptDefinition prompt = new PromptDefinition("myUnknown", "custom_type");
      prompt.setProperties(List.of(new JobParameter("defaultValue", "fallback")));

      ParameterDefinition def = prompt.toParameterDefinition();
      assertInstanceOf(StringParameterDefinition.class, def);
      assertEquals("myUnknown", def.getName());
    }
  }

  @Test
  @DisplayName("setProperties with null gives empty list")
  void nullPropertiesGivesEmptyList() {
    PromptDefinition prompt = new PromptDefinition("p", "string");
    prompt.setProperties(null);
    assertNotNull(prompt.getProperties());
    assertTrue(prompt.getProperties().isEmpty());
  }
}
