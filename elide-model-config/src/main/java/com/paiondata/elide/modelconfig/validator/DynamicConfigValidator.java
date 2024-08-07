/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.modelconfig.validator;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.SecurityCheck;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.EntityDictionaryBuilderCustomizer;
import com.paiondata.elide.core.dictionary.EntityPermissions;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.security.checks.Check;
import com.paiondata.elide.core.security.checks.FilterExpressionCheck;
import com.paiondata.elide.core.security.checks.UserCheck;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.modelconfig.Config;
import com.paiondata.elide.modelconfig.DynamicConfigHelpers;
import com.paiondata.elide.modelconfig.DynamicConfigSchemaValidator;
import com.paiondata.elide.modelconfig.DynamicConfiguration;
import com.paiondata.elide.modelconfig.io.FileLoader;
import com.paiondata.elide.modelconfig.model.Argument;
import com.paiondata.elide.modelconfig.model.DBConfig;
import com.paiondata.elide.modelconfig.model.Dimension;
import com.paiondata.elide.modelconfig.model.ElideDBConfig;
import com.paiondata.elide.modelconfig.model.ElideNamespaceConfig;
import com.paiondata.elide.modelconfig.model.ElideSQLDBConfig;
import com.paiondata.elide.modelconfig.model.ElideSecurityConfig;
import com.paiondata.elide.modelconfig.model.ElideTableConfig;
import com.paiondata.elide.modelconfig.model.Join;
import com.paiondata.elide.modelconfig.model.Measure;
import com.paiondata.elide.modelconfig.model.Named;
import com.paiondata.elide.modelconfig.model.NamespaceConfig;
import com.paiondata.elide.modelconfig.model.Table;
import com.paiondata.elide.modelconfig.model.TableSource;
import com.paiondata.elide.modelconfig.store.models.ConfigFile;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import lombok.Getter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Util class to validate and parse the config files. Optionally compiles config files.
 */
public class DynamicConfigValidator implements DynamicConfiguration, Validator {

    private static final Set<String> SQL_DISALLOWED_WORDS = new HashSet<>(
            Arrays.asList("DROP", "TRUNCATE", "DELETE", "INSERT", "UPDATE", "ALTER", "COMMENT", "CREATE", "DESCRIBE",
                    "SHOW", "USE", "GRANT", "REVOKE", "CONNECT", "LOCK", "EXPLAIN", "CALL", "MERGE", "RENAME"));
    private static final String SQL_SPLIT_REGEX = "\\s+";
    private static final String SEMI_COLON = ";";
    private static final Pattern HANDLEBAR_REGEX = Pattern.compile("<%(.*?)%>");

    @Getter private final ElideTableConfig elideTableConfig = new ElideTableConfig();
    @Getter private ElideSecurityConfig elideSecurityConfig;
    @Getter private Map<String, Object> modelVariables;
    private Map<String, Object> dbVariables;
    @Getter private final ElideDBConfig elideSQLDBConfig = new ElideSQLDBConfig();
    @Getter private final ElideNamespaceConfig elideNamespaceConfig = new ElideNamespaceConfig();
    private final DynamicConfigSchemaValidator schemaValidator = new DynamicConfigSchemaValidator();
    private final EntityDictionary dictionary;
    private final FileLoader fileLoader;

    private static final Pattern FILTER_VARIABLE_PATTERN = Pattern.compile(".*?\\{\\{(\\w+)\\}\\}");

    public DynamicConfigValidator(ClassScanner scanner, String configDir) {
        this(builder -> builder.scanner(scanner), configDir);
    }

    public DynamicConfigValidator(
            EntityDictionaryBuilderCustomizer entityDictionaryBuilderCustomizer,
            String configDir) {
        this(buildEntityDictionary(entityDictionaryBuilderCustomizer), configDir);
    }

    public DynamicConfigValidator(EntityDictionary dictionary, String configDir) {
        this.dictionary = dictionary;
        this.fileLoader = new FileLoader(configDir);
        initialize();
    }

    protected static EntityDictionary buildEntityDictionary(
            EntityDictionaryBuilderCustomizer entityDictionaryBuilderCustomizer) {
        EntityDictionary.EntityDictionaryBuilder  builder = EntityDictionary.builder();
        if (entityDictionaryBuilderCustomizer != null) {
            entityDictionaryBuilderCustomizer.customize(builder);
        }
        return builder.build();
    }

    private void initialize() {
        Set<Class<?>> annotatedClasses =
                        dictionary.getScanner().getAnnotatedClasses(Arrays.asList(Include.class, SecurityCheck.class));

        annotatedClasses.forEach(cls -> {
            if (cls.getAnnotation(Include.class) != null) {
                dictionary.bindEntity(cls);
            } else {
                dictionary.addSecurityCheck(cls);
            }
        });
    }

    public static void main(String[] args) {
        Options options = prepareOptions();

        try {
            CommandLine cli = new DefaultParser().parse(options, args);

            if (cli.hasOption("help")) {
                printHelp(options);
                System.exit(0);
            }
            if (!cli.hasOption("configDir")) {
                printHelp(options);
                System.err.println("Missing required option");
                System.exit(1);
            }
            String configDir = cli.getOptionValue("configDir");

            DynamicConfigValidator dynamicConfigValidator =
                    new DynamicConfigValidator(new DefaultClassScanner(), configDir);
            dynamicConfigValidator.readAndValidateConfigs();
            System.out.println("Configs Validation Passed!");
            System.exit(0);

        } catch (Exception e) {
            String msg = isBlank(e.getMessage()) ? "Process Failed!" : e.getMessage();
            System.err.println(msg);
            System.exit(2);
        }
    }

    @Override
    public void validate(Map<String, ConfigFile> resourceMap) {

        resourceMap.forEach((path, file) -> {
            if (file.getContent() == null || file.getContent().isEmpty()) {
                throw new BadRequestException(String.format("Null or empty file content for %s", file.getPath()));
            }

            //Validate that all the files are ones we know about and are safe to manipulate...
            if (file.getType().equals(ConfigFile.ConfigFileType.UNKNOWN)) {
                throw new BadRequestException(String.format("Unrecognized File: %s", file.getPath()));
            }

            if (path.contains("..")) {
                throw new BadRequestException(String.format("Parent directory traversal not allowed: %s",
                        file.getPath()));
            }

            //Validate that the file types and file paths match...
            if (! file.getType().equals(FileLoader.toType(path))) {
                throw new BadRequestException(String.format("File type %s does not match file path: %s",
                        file.getType(), file.getPath()));
            }
        });

        readConfigs(resourceMap);
        validateConfigs();
    }

    /**
     * Read and validate config files under config directory.
     * @throws IOException IOException
     */
    public void readAndValidateConfigs() throws IOException {
        Map<String, ConfigFile> loadedFiles = fileLoader.loadResources();

        validate(loadedFiles);
    }

    public void readConfigs() throws IOException {
        readConfigs(fileLoader.loadResources());
    }

    public void readConfigs(Map<String, ConfigFile> resourceMap) {
        this.modelVariables = readVariableConfig(Config.MODELVARIABLE, resourceMap);
        this.elideSecurityConfig = readSecurityConfig(resourceMap);
        this.dbVariables = readVariableConfig(Config.DBVARIABLE, resourceMap);
        this.elideSQLDBConfig.setDbconfigs(readDbConfig(resourceMap));
        this.elideTableConfig.setTables(readTableConfig(resourceMap));
        this.elideNamespaceConfig.setNamespaceconfigs(readNamespaceConfig(resourceMap));
        populateInheritance(this.elideTableConfig);
    }

    public void validateConfigs() {
        validateSecurityConfig();
        boolean configurationExists = validateRequiredConfigsProvided();

        if (configurationExists) {
            validateNameUniqueness(this.elideSQLDBConfig.getDbconfigs(),
                    "Multiple DB configs found with the same name: ");
            validateNameUniqueness(this.elideTableConfig.getTables(),
                    "Multiple Table configs found with the same name: ");
            validateTableConfig();
            validateNameUniqueness(this.elideNamespaceConfig.getNamespaceconfigs(),
                    "Multiple Namespace configs found with the same name: ");
            validateNamespaceConfig();
            validateJoinedTablesDBConnectionName(this.elideTableConfig);
        }
    }

    @Override
    public Set<Table> getTables() {
        return elideTableConfig.getTables();
    }

    @Override
    public Set<String> getRoles() {
        return elideSecurityConfig.getRoles();
    }

    @Override
    public Set<DBConfig> getDatabaseConfigurations() {
        return elideSQLDBConfig.getDbconfigs();
    }

    @Override
    public Set<NamespaceConfig> getNamespaceConfigurations() {
        return elideNamespaceConfig.getNamespaceconfigs();
    }

    private static void validateInheritance(ElideTableConfig tables) {
        tables.getTables().stream().forEach(table -> validateInheritance(tables, table, new HashSet<>()));
    }

    private static void validateInheritance(ElideTableConfig tables, Table table, Set<Table> visited) {
        visited.add(table);

        if (!table.hasParent()) {
            return;
        }
        Table parent = table.getParent(tables);
        if (parent == null) {
            throw new IllegalStateException(
                    "Undefined model: " + table.getExtend() + " is used as a Parent(extend) for another model.");
        }
        if (visited.contains(parent)) {
            throw new IllegalStateException(
                    String.format("Inheriting from table '%s' creates an illegal cyclic dependency.",
                            parent.getName()));
        }
        validateInheritance(tables, parent, visited);
    }

    private void populateInheritance(ElideTableConfig elideTableConfig) {
        //ensures validation is run before populate always.
        validateInheritance(this.elideTableConfig);

        Set<Table> processed = new HashSet<>();
        elideTableConfig.getTables().stream().forEach(table -> populateInheritance(table, processed));
    }

    private void populateInheritance(Table table, Set<Table> processed) {
        if (processed.contains(table)) {
            return;
        }

        processed.add(table);

        if (!table.hasParent()) {
            return;
        }

        Table parent = table.getParent(this.elideTableConfig);
        if (!processed.contains(parent)) {
            populateInheritance(parent, processed);
        }

        Map<String, Measure> measures = getInheritedMeasures(parent, attributesListToMap(table.getMeasures()));
        table.setMeasures(new ArrayList<>(measures.values()));

        Map<String, Dimension> dimensions = getInheritedDimensions(parent, attributesListToMap(table.getDimensions()));
        table.setDimensions(new ArrayList<>(dimensions.values()));

        Map<String, Join> joins = getInheritedJoins(parent, attributesListToMap(table.getJoins()));
        table.setJoins(new ArrayList<>(joins.values()));

        String schema = getInheritedSchema(parent, table.getSchema());
        table.setSchema(schema);

        String dbConnectionName = getInheritedConnection(parent, table.getDbConnectionName());
        table.setDbConnectionName(dbConnectionName);

        String sql = getInheritedSql(parent, table.getSql());
        table.setSql(sql);

        String tableName = getInheritedTable(parent, table.getTable());
        table.setTable(tableName);

        List<Argument> arguments = getInheritedArguments(parent, table.getArguments());
        table.setArguments(arguments);
        // isFact, isHidden, ReadAccess, namespace have default Values in schema, so can not be inherited.
        // Other properties (tags, cardinality, etc.) have been categorized as non-inheritable too.
    }

    private <T extends Named> Map<String, T> attributesListToMap(List<T> attributes) {
        return attributes.stream().collect(Collectors.toMap(T::getName, attribute -> attribute));
    }

    @FunctionalInterface
    public interface Inheritance<T> {
        public T inherit();
    }

    private Map<String, Measure> getInheritedMeasures(Table table, Map<String, Measure> measures) {
        Inheritance<?> action = () -> {
            table.getMeasures().forEach(measure -> {
                if (!measures.containsKey(measure.getName())) {
                    measures.put(measure.getName(), measure);
                }
            });
            return measures;
        };

        action.inherit();
        return measures;
    }

    private Map<String, Dimension> getInheritedDimensions(Table table, Map<String, Dimension> dimensions) {
        Inheritance<?> action = () -> {
            table.getDimensions().forEach(dimension -> {
                if (!dimensions.containsKey(dimension.getName())) {
                    dimensions.put(dimension.getName(), dimension);
                }
            });
            return dimensions;
        };

        action.inherit();
        return dimensions;
    }

    private Map<String, Join> getInheritedJoins(Table table, Map<String, Join> joins) {
        Inheritance<?> action = () -> {
            table.getJoins().forEach(join -> {
                if (!joins.containsKey(join.getName())) {
                    joins.put(join.getName(), join);
                }
            });
            return joins;
        };

        action.inherit();
        return joins;
    }

    private <T> T getInheritedAttribute(Inheritance<T> action, T property) {
        return property == null ? action.inherit() : property;
    }

    private <T extends Collection<?>> T getInheritedAttribute(Inheritance<T> action, T property) {
        return property == null || property.isEmpty() ? action.inherit() : property;
    }

    private String getInheritedSchema(Table table, String schema) {
        return getInheritedAttribute(table::getSchema, schema);
    }

    private String getInheritedConnection(Table table, String connection) {
        return getInheritedAttribute(table::getDbConnectionName, connection);
    }

    private String getInheritedSql(Table table, String sql) {
        return getInheritedAttribute(table::getSql, sql);
    }

    private String getInheritedTable(Table table, String tableName) {
        return getInheritedAttribute(table::getTable, tableName);
    }

    private List<Argument> getInheritedArguments(Table table, List<Argument> arguments) {
        return getInheritedAttribute(table::getArguments, arguments);
    }

    /**
     * Read variable file config.
     * @param config Config Enum
     * @return Map<String, Object> A map containing all the variables if variable config exists else empty map
     */
    private Map<String, Object> readVariableConfig(Config config, Map<String, ConfigFile> resourceMap) {

        return resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(config.getConfigPath()))
                        .map(entry -> {
                            try {
                                return DynamicConfigHelpers.stringToVariablesPojo(entry.getKey(),
                                                entry.getValue().getContent(), schemaValidator);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e.getMessage(), e);
                            }
                        })
                        .findFirst()
                        .orElse(new HashMap<>());
    }

    /**
     * Read and validates security config file.
     */
    private ElideSecurityConfig readSecurityConfig(Map<String, ConfigFile> resourceMap) {

        return resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(Config.SECURITY.getConfigPath()))
                        .map(entry -> {
                            try {
                                String content = entry.getValue().getContent();
                                validateConfigForMissingVariables(content, this.modelVariables);
                                return DynamicConfigHelpers.stringToElideSecurityPojo(entry.getKey(),
                                                content, this.modelVariables, schemaValidator);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e.getMessage(), e);
                            }
                        })
                        .findAny()
                        .orElse(new ElideSecurityConfig());
    }

    /**
     * Read and validates db config files.
     * @return Set<DBConfig> Set of SQL DB Configs
     */
    private Set<DBConfig> readDbConfig(Map<String, ConfigFile> resourceMap) {

        return resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(Config.SQLDBConfig.getConfigPath()))
                        .map(entry -> {
                            try {
                                String content = entry.getValue().getContent();
                                validateConfigForMissingVariables(content, this.dbVariables);
                                return DynamicConfigHelpers.stringToElideDBConfigPojo(entry.getKey(),
                                                content, this.dbVariables, schemaValidator);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e.getMessage(), e);
                            }
                        })
                        .flatMap(dbconfig -> dbconfig.getDbconfigs().stream())
                        .collect(Collectors.toSet());
    }

    /**
     * Read and validates namespace config files.
     * @return Set<NamespaceConfig> Set of Namespace Configs
     */
    private Set<NamespaceConfig> readNamespaceConfig(Map<String, ConfigFile> resourceMap) {

        return resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(Config.NAMESPACEConfig.getConfigPath()))
                        .map(entry -> {
                            try {
                                String content = entry.getValue().getContent();
                                validateConfigForMissingVariables(content, this.modelVariables);
                                String fileName = entry.getKey();
                                return DynamicConfigHelpers.stringToElideNamespaceConfigPojo(fileName,
                                                content, this.modelVariables, schemaValidator);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e.getMessage(), e);
                            }
                        })
                        .flatMap(namespaceconfig -> namespaceconfig.getNamespaceconfigs().stream())
                        .collect(Collectors.toSet());
    }

    /**
     * Read and validates table config files.
     */
    private Set<Table> readTableConfig(Map<String, ConfigFile> resourceMap) {

        return resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(Config.TABLE.getConfigPath()))
                        .map(entry -> {
                            try {
                                String content = entry.getValue().getContent();
                                validateConfigForMissingVariables(content, this.modelVariables);
                                return DynamicConfigHelpers.stringToElideTablePojo(entry.getKey(),
                                                content, this.modelVariables, schemaValidator);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e.getMessage(), e);
                            }
                        })
                        .flatMap(table -> table.getTables().stream())
                        .collect(Collectors.toSet());
    }

    /**
     * Checks if neither Table nor DB config files provided.
     */
    private boolean validateRequiredConfigsProvided() {
        return !(this.elideTableConfig.getTables().isEmpty() && this.elideSQLDBConfig.getDbconfigs().isEmpty());
    }

    /**
     * Extracts any handlebar variables in config file and checks if they are
     * defined in variable config. Throw exception for undefined variables.
     * @param config config file
     * @param variables A map of defined variables
     */
    private static void validateConfigForMissingVariables(String config, Map<String, Object> variables) {
        Matcher regexMatcher = HANDLEBAR_REGEX.matcher(config);
        while (regexMatcher.find()) {
            String str = regexMatcher.group(1).trim();
            if (!variables.containsKey(str)) {
                throw new IllegalStateException(str + " is used as a variable in either table or security config files "
                        + "but is not defined in variables config file.");
            }
        }
    }

    /**
     * Validate table configs.
     * @return boolean true if all provided table properties passes validation
     */
    private boolean validateTableConfig() {
        Set<String> extractedFieldChecks = new HashSet<>();
        Set<String> extractedTableChecks = new HashSet<>();
        PermissionExpressionVisitor visitor = new PermissionExpressionVisitor();

        for (Table table : elideTableConfig.getTables()) {

            validateSql(table.getSql());
            validateArguments(table, table.getArguments(), table.getFilterTemplate());
            //TODO - once tables support versions - replace NO_VERSION with apiVersion
            validateNamespaceExists(table.getNamespace(), NO_VERSION);
            Set<String> tableFields = new HashSet<>();

            table.getDimensions().forEach(dim -> {
                validateFieldNameUniqueness(tableFields, dim.getName(), table.getName());
                validateSql(dim.getDefinition());
                validateTableSource(dim.getTableSource());
                validateArguments(table, dim.getArguments(), dim.getFilterTemplate());
                extractChecksFromExpr(dim.getReadAccess(), extractedFieldChecks, visitor);
            });

            table.getMeasures().forEach(measure -> {
                validateFieldNameUniqueness(tableFields, measure.getName(), table.getName());
                validateSql(measure.getDefinition());
                validateArguments(table, measure.getArguments(), measure.getFilterTemplate());
                extractChecksFromExpr(measure.getReadAccess(), extractedFieldChecks, visitor);
            });

            table.getJoins().forEach(join -> {
                validateFieldNameUniqueness(tableFields, join.getName(), table.getName());
                validateSql(join.getDefinition());
                validateModelExists(join.getTo());
                //TODO - once tables support versions - replace NO_VERSION with apiVersion
                validateNamespaceExists(join.getNamespace(), NO_VERSION);
            });

            extractChecksFromExpr(table.getReadAccess(), extractedTableChecks, visitor);
        }

        validateChecks(extractedTableChecks, extractedFieldChecks);

        return true;
    }

    /**
     * Validate namespace configs.
     * @return boolean true if all provided namespace properties passes validation
     */
    private boolean validateNamespaceConfig() {
        Set<String> extractedChecks = new HashSet<>();
        PermissionExpressionVisitor visitor = new PermissionExpressionVisitor();

        for (NamespaceConfig namespace : elideNamespaceConfig.getNamespaceconfigs()) {
            extractChecksFromExpr(namespace.getReadAccess(), extractedChecks, visitor);
        }

        validateChecks(extractedChecks, Collections.emptySet());

        return true;
    }

    private void validateArguments(Table table, List<Argument> arguments, String requiredFilter) {
        List<Argument> allArguments = new ArrayList<>(arguments);

        /* Check for table arguments added in the required filter template */
        if (requiredFilter != null) {
            Matcher matcher = FILTER_VARIABLE_PATTERN.matcher(requiredFilter);
            while (matcher.find()) {
                allArguments.add(Argument.builder()
                        .name(matcher.group(1))
                        .build());
            }
        }

        validateNameUniqueness(allArguments, "Multiple Arguments found with the same name: ");
        arguments.forEach(arg -> validateTableSource(arg.getTableSource()));
    }

    private void validateChecks(Set<String> tableChecks, Set<String> fieldChecks) {

        if (tableChecks.isEmpty() && fieldChecks.isEmpty()) {
            return; // Nothing to validate
        }

        Set<String> staticChecks = dictionary.getCheckIdentifiers();

        List<String> undefinedChecks = Stream.concat(tableChecks.stream(), fieldChecks.stream())
                        .filter(check -> !(elideSecurityConfig.hasCheckDefined(check) || staticChecks.contains(check)))
                        .sorted()
                        .collect(Collectors.toList());

        if (!undefinedChecks.isEmpty()) {
            throw new IllegalStateException("Found undefined security checks: " + undefinedChecks);
        }

        tableChecks.stream()
                .filter(check -> dictionary.getCheckMappings().containsKey(check))
                .forEach(check -> {
                    Class<? extends Check> checkClass = dictionary.getCheck(check);
                    //Validates if the permission check either user Check or FilterExpressionCheck Check
                    if (!(UserCheck.class.isAssignableFrom(checkClass)
                            || FilterExpressionCheck.class.isAssignableFrom(checkClass))) {
                        throw new IllegalStateException("Table or Namespace cannot have Operation Checks. Given: "
                                + checkClass);
                    }
                });
        fieldChecks.stream()
                .filter(check -> dictionary.getCheckMappings().containsKey(check))
                .forEach(check -> {
                    Class<? extends Check> checkClass = dictionary.getCheck(check);
                    //Validates if the permission check is User check
                    if (!UserCheck.class.isAssignableFrom(checkClass)) {
                        throw new IllegalStateException("Field can only have User checks or Roles. Given: "
                                + checkClass);
                    }
                });
    }

    private static void extractChecksFromExpr(String readAccess, Set<String> extractedChecks,
                    PermissionExpressionVisitor visitor) {
        if (isNotBlank(readAccess)) {
            ParseTree root = EntityPermissions.parseExpression(readAccess);
            extractedChecks.addAll(visitor.visit(root));
        }
    }

    private static void validateFieldNameUniqueness(Set<String> alreadyFoundFields, String fieldName,
                    String tableName) {
        if (!alreadyFoundFields.add(fieldName.toLowerCase(Locale.ENGLISH))) {
            throw new IllegalStateException(String.format("Duplicate!! Field name: %s is not unique for table: %s",
                            fieldName, tableName));
        }
    }

    /**
     * Validates tableSource is in format: modelName.logicalColumnName and refers to a defined model and a defined
     * column with in that model.
     */
    private void validateTableSource(TableSource tableSource) {
        if (tableSource == null) {
            return; // Nothing to validate
        }

        String modelName = Table.getModelName(tableSource.getTable(), tableSource.getNamespace());

        if (elideTableConfig.hasTable(modelName)) {
            Table lookupTable = elideTableConfig.getTable(modelName);
            if (!lookupTable.hasField(tableSource.getColumn())) {
                throw new IllegalStateException("Invalid tableSource : "
                        + tableSource
                        + " . Field : "
                        + tableSource.getColumn()
                        + " is undefined for hjson model: "
                        + tableSource.getTable());
            }
            return;
        }

        //TODO - once tables support versions - replace NO_VERSION with apiVersion
        if (hasStaticModel(modelName, NO_VERSION)) {
            if (!hasStaticField(modelName, NO_VERSION, tableSource.getColumn())) {
                throw new IllegalStateException("Invalid tableSource : " + tableSource
                        + " . Field : " + tableSource.getColumn()
                        + " is undefined for non-hjson model: " + tableSource.getTable());
            }
            return;
        }

        throw new IllegalStateException("Invalid tableSource : " + tableSource
                + " . Undefined model: " + tableSource.getTable());
    }

    /**
     * Validates join clause does not refer to a Table which is not in the same DBConnection. If joined table is not
     * part of dynamic configuration, then ignore
     */
    private static void validateJoinedTablesDBConnectionName(ElideTableConfig elideTableConfig) {

        for (Table table : elideTableConfig.getTables()) {
            if (!table.getJoins().isEmpty()) {

                Set<String> joinedTables = table.getJoins()
                        .stream()
                        //TODO - NOT SURE
                        .map(Join::getTo)
                        .collect(Collectors.toSet());

                Set<String> connections = elideTableConfig.getTables()
                        .stream()
                        .filter(t -> joinedTables.contains(t.getGlobalName()))
                        .map(Table::getDbConnectionName)
                        .collect(Collectors.toSet());

                if (connections.size() > 1 || (connections.size() == 1
                                && !Objects.equals(table.getDbConnectionName(), connections.iterator().next()))) {
                    throw new IllegalStateException("DBConnection name mismatch between table: " + table.getName()
                                    + " and tables in its Join Clause.");
                }
            }
        }
    }

    /**
     * Validates table (or db connection) name is unique across all the dynamic table (or db connection) configs.
     */
    public static void validateNameUniqueness(Collection<? extends Named> configs, String errorMsg) {

        Set<String> names = new HashSet<>();
        configs.forEach(obj -> {
            if (!names.add(obj.getGlobalName().toLowerCase(Locale.ENGLISH))) {
                throw new IllegalStateException(errorMsg + obj.getGlobalName());
            }
        });
    }

    /**
     * Check if input sql definition contains either semicolon or any of disallowed
     * keywords. Throw exception if check fails.
     */
    private static void validateSql(String sqlDefinition) {
        if (isNotBlank(sqlDefinition) && (sqlDefinition.contains(SEMI_COLON)
                || containsDisallowedWords(sqlDefinition, SQL_SPLIT_REGEX, SQL_DISALLOWED_WORDS))) {
            throw new IllegalStateException("sql/definition provided in table config contain either '" + SEMI_COLON
                    + "' or one of these words: " + Arrays.toString(SQL_DISALLOWED_WORDS.toArray()));
        }
    }

    /**
     * Validate role name provided in security config.
     * @return boolean true if all role name passes validation else throw exception
     */
    private boolean validateSecurityConfig() {
        Set<String> alreadyDefinedRoles = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        alreadyDefinedRoles.addAll(dictionary.getCheckIdentifiers());

        elideSecurityConfig.getRoles().forEach(role -> {
            if (alreadyDefinedRoles.contains(role)) {
                throw new IllegalStateException(String.format(
                                "Duplicate!! Role name: '%s' is already defined. Please use different role.", role));
            }
            alreadyDefinedRoles.add(role);
        });

        return true;
    }

    private void validateModelExists(String name) {
        if (!(elideTableConfig.hasTable(name) || hasStaticModel(name, NO_VERSION))) {
            throw new IllegalStateException(
                            "Model: " + name + " is neither included in dynamic models nor in static models");
        }
    }

    private void validateNamespaceExists(String name, String version) {
        if (!elideNamespaceConfig.hasNamespace(name, version)) {
            throw new IllegalStateException(
                            "Namespace: " + name + " is not included in dynamic configs");
        }
    }

    /**
     * Checks if any word in the input string matches any of the disallowed words.
     * @param str input string to validate
     * @param splitter regex for splitting input string
     * @param keywords Set of disallowed words
     * @return boolean true if any word in the input string matches any of the
     *         disallowed words else false
     */
    private static boolean containsDisallowedWords(String str, String splitter, Set<String> keywords) {
        return isNotBlank(str)
                && Arrays.stream(str.trim().toUpperCase(Locale.ENGLISH).split(splitter)).anyMatch(keywords::contains);
    }

    /**
     * Define Arguments.
     */
    private static final Options prepareOptions() {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "Print a help message and exit."));
        options.addOption(new Option("c", "configDir", true,
                "Path for Configs Directory.\n"
                        + "Expected Directory Structure under Configs Directory:\n"
                        + "./models/security.hjson(optional)\n"
                        + "./models/variables.hjson(optional)\n"
                        + "./models/tables/(optional)\n"
                        + "./models/tables/table1.hjson\n"
                        + "./models/tables/table2.hjson\n"
                        + "./models/tables/tableN.hjson\n"
                        + "./db/variables.hjson(optional)\n"
                        + "./db/sql/(optional)\n"
                        + "./db/sql/db1.hjson\n"
                        + "./db/sql/db2.hjson\n"
                        + "./db/sql/dbN.hjson\n"));

        return options;
    }

    /**
     * Print Help.
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                "java -cp <Jar File> com.paiondata.elide.modelconfig.validator.DynamicConfigValidator",
                options);
    }

    private boolean hasStaticField(String modelName, String version, String fieldName) {
        Type<?> modelType = dictionary.getEntityClass(modelName, version);
        if (modelType == null) {
            return false;
        }

        try {
            return (modelType.getDeclaredField(fieldName) != null);
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private boolean hasStaticModel(String modelName, String version) {
        Type<?> modelType = dictionary.getEntityClass(modelName, version);
        return modelType != null;
    }
}
