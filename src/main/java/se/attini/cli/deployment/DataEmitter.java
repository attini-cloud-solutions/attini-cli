package se.attini.cli.deployment;

import static java.util.Objects.requireNonNull;
import static se.attini.cli.PrintItem.PrintType.NORMAL_SAME_LINE;
import static se.attini.cli.PrintItem.message;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import se.attini.cli.ConsolePrinter;
import se.attini.cli.PrintItem;
import se.attini.cli.PrintUtil;
import se.attini.cli.global.GlobalConfig;


public class DataEmitter {
    private final GlobalConfig globalConfig;
    private final ObjectMapper objectMapper;
    private final ConsolePrinter consolePrinter;


    public DataEmitter(GlobalConfig globalConfig,
                       ObjectMapper objectMapper,
                       ConsolePrinter consolePrinter) {
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    public void emitNewLine() {
        if (!globalConfig.printAsJson()) {
            consolePrinter.print(PrintItem.newLine());
        }
    }

    public void emitKeyValueSameLine(String key, String value, PrintUtil.Color color) {
        if (globalConfig.printAsJson()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("timestamp", Instant.now().toEpochMilli())
                      .put("type", "object")
                      .set("data", objectMapper.createObjectNode().put(key, value));
            consolePrinter.print(message(objectNode.toString()));
        } else {
            consolePrinter.print(message(NORMAL_SAME_LINE, key + ": " + PrintUtil.toColor(value, color)));
        }

    }

    public void emitString(String value) {
        if (globalConfig.printAsJson()) {
            ObjectNode objectNode = objectMapper.createObjectNode()
                                                .put("timestamp", Instant.now().toEpochMilli())
                                                .put("type", "string")
                                                .put("data", value.trim());
            consolePrinter.print(message(objectNode.toString()));
        } else {
            consolePrinter.print(message(value));
        }
    }

    public void emitPrintItem(PrintItem printItem) {
        if (globalConfig.printAsJson()) {
            ObjectNode objectNode = objectMapper.createObjectNode()
                                                .put("timestamp", Instant.now().toEpochMilli())
                                                .put("type", "string")
                                                .put("data", printItem.getMessage().trim());
            consolePrinter.print(message(objectNode.toString()));
        } else {
            consolePrinter.print(printItem);
        }
    }


    public void emitKeyValue(String key, String value) {
    emitKeyValue(key, value, PrintUtil.Color.DEFAULT);
    }

    public void emitKeyValue(String key, String value, PrintUtil.Color color) {
        if (globalConfig.printAsJson()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("timestamp", Instant.now().toEpochMilli())
                      .put("type", "object")
                      .set("data", objectMapper.createObjectNode().put(key, value));

            consolePrinter.print(message(objectNode.toString()));
        } else {
            consolePrinter.print(message(key + ": " + PrintUtil.toColor(value, color)));
        }
    }



}
