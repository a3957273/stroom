/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package stroom.test;

import stroom.dashboard.impl.DashboardStore;
import stroom.data.shared.StreamTypeNames;
import stroom.data.store.api.Store;
import stroom.data.store.impl.fs.FsVolumeService;
import stroom.data.store.impl.fs.shared.FindFsVolumeCriteria;
import stroom.data.store.impl.fs.shared.FsVolume;
import stroom.docref.DocRef;
import stroom.entity.shared.ExpressionCriteria;
import stroom.feed.api.FeedProperties;
import stroom.feed.api.FeedStore;
import stroom.importexport.impl.ImportExportSerializer;
import stroom.importexport.shared.ImportState.ImportMode;
import stroom.index.impl.IndexStore;
import stroom.index.impl.IndexVolumeGroupService;
import stroom.index.impl.IndexVolumeService;
import stroom.index.shared.IndexVolume;
import stroom.meta.shared.MetaFields;
import stroom.pipeline.PipelineStore;
import stroom.processor.api.ProcessorFilterService;
import stroom.processor.api.ProcessorService;
import stroom.processor.shared.Processor;
import stroom.processor.shared.ProcessorFilter;
import stroom.processor.shared.QueryData;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm;
import stroom.statistics.impl.hbase.entity.StroomStatsStoreStore;
import stroom.statistics.impl.sql.entity.StatisticStoreStore;
import stroom.test.common.StroomCoreServerTestFileUtil;
import stroom.testdata.DataGenerator;
import stroom.testdata.FlatDataWriterBuilder;
import stroom.util.io.FileUtil;
import stroom.util.io.StreamUtil;
import stroom.util.logging.LogUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Script to create some base data for testing.
 */
public final class SetupSampleDataBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetupSampleDataBean.class);

    private static final String ROOT_DIR_NAME = "samples";

    private static final String STATS_COUNT_FEED_LARGE_NAME = "COUNT_FEED_LARGE";
    private static final String STATS_COUNT_FEED_SMALL_NAME = "COUNT_FEED_SMALL";
    private static final String STATS_VALUE_FEED_LARGE_NAME = "VALUE_FEED_LARGE";
    private static final String STATS_VALUE_FEED_SMALL_NAME = "VALUE_FEED_SMALL";
    // 52,000 is just over 3 days at 5000ms intervals
    private static final int STATS_ITERATIONS_LARGE = 52_000;
    // 1,000 is just over 1hr at 5000ms intervals
    private static final int STATS_ITERATIONS_SMALL = 1_000;
    private static final String STATS_COUNT_API_FEED_NAME = "COUNT_V3";
    private static final String STATS_COUNT_API_DATA_FILE = "./stroom-app/src/test/resources/SetupSampleDataBean_COUNT_V3.xml";

    private static final int LOAD_CYCLES = 10;

    private final FeedStore feedStore;
    private final FeedProperties feedProperties;
    private final Store streamStore;
    private final ImportExportSerializer importExportSerializer;
    private final ProcessorService processorService;
    private final ProcessorFilterService processorFilterService;
    private final PipelineStore pipelineStore;
    private final DashboardStore dashboardStore;
    private final IndexStore indexStore;
    private final IndexVolumeService indexVolumeService;
    private final IndexVolumeGroupService indexVolumeGroupService;
    private final FsVolumeService fsVolumeService;
    private final StatisticStoreStore statisticStoreStore;
    private final StroomStatsStoreStore stroomStatsStoreStore;

    @Inject
    SetupSampleDataBean(final FeedStore feedStore,
                        final FeedProperties feedProperties,
                        final Store streamStore,
                        final ImportExportSerializer importExportSerializer,
                        final ProcessorService processorService,
                        final ProcessorFilterService processorFilterService,
                        final PipelineStore pipelineStore,
                        final DashboardStore dashboardStore,
                        final IndexStore indexStore,
                        final IndexVolumeService indexVolumeService,
                        final IndexVolumeGroupService indexVolumeGroupService,
                        final FsVolumeService fsVolumeService,
                        final StatisticStoreStore statisticStoreStore,
                        final StroomStatsStoreStore stroomStatsStoreStore) {
        this.feedStore = feedStore;
        this.feedProperties = feedProperties;
        this.streamStore = streamStore;
        this.importExportSerializer = importExportSerializer;
        this.processorService = processorService;
        this.processorFilterService = processorFilterService;
        this.pipelineStore = pipelineStore;
        this.dashboardStore = dashboardStore;
        this.indexStore = indexStore;
        this.indexVolumeService = indexVolumeService;
        this.indexVolumeGroupService = indexVolumeGroupService;
        this.fsVolumeService = fsVolumeService;
        this.statisticStoreStore = statisticStoreStore;
        this.stroomStatsStoreStore = stroomStatsStoreStore;
    }

//    private void createStreamAttributes() {
//        final BaseResultList<StreamAttributeKey> list = metaKeyService
//                .find(new FindStreamAttributeKeyCriteria());
//        final HashSet<String> existingItems = new HashSet<>();
//        for (final StreamAttributeKey streamAttributeKey : list) {
//            existingItems.add(streamAttributeKey.getName());
//        }
//        for (final String name : StreamAttributeConstants.SYSTEM_ATTRIBUTE_FIELD_TYPE_MAP.keySet()) {
//            if (!existingItems.contains(name)) {
//                try {
//                    metaKeyService.save(new StreamAttributeKey(name,
//                            StreamAttributeConstants.SYSTEM_ATTRIBUTE_FIELD_TYPE_MAP.get(name)));
//                } catch (final RuntimeException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    public void run(final boolean shutdown) {
        // Ensure admin user exists.
//        LOGGER.info("Creating admin user");
//        authenticationService.getUser(new AuthenticationToken("admin", null));

//        createRandomExplorerNode(null, "", 0, 2);

        checkVolumesExist();

        // Sample data/config can exist in many projects so here we define all
        // the root directories that we want to
        // process
        final Path coreServerSamplesDir = StroomCoreServerTestFileUtil.getTestResourcesDir()
                .resolve(ROOT_DIR_NAME);
        final Path statisticsSamplesDir = Paths.get("./stroom-statistics/stroom-statistics-impl/src/test/resources")
                .resolve(ROOT_DIR_NAME);

        final Path[] rootDirs = new Path[] {
                coreServerSamplesDir,
                statisticsSamplesDir};

        // process each root dir in turn, importing content and loading data into feeds
        for (final Path dir : rootDirs) {
            loadDirectory(shutdown, dir);
        }

        //Additional content is loaded by the gradle build in task downloadStroomContent

        // Add volumes to all indexes.
        final List<DocRef> indexList = indexStore.list();
        logDocRefs(indexList, "indexes");

        // TODO replace this with new volumes index
//        for (final DocRef indexRef : indexList) {
//            indexVolumeService.setVolumesForIndex(indexRef, volumeSet);
//        }

        // Create index pipeline processor filters
        createIndexingProcessorFilter("Example index", StreamTypeNames.EVENTS, Optional.empty());
        createIndexingProcessorFilter(
                "LAX_CARGO_VOLUME-INDEX", StreamTypeNames.RECORDS, Optional.of("LAX_CARGO_VOLUME"));
        createIndexingProcessorFilter(
                "BROADBAND_SPEED_TESTS-INDEX", StreamTypeNames.RECORDS, Optional.of("BROADBAND_SPEED_TESTS"));

        createAndLoadGeneratedData(coreServerSamplesDir.resolve("generated").resolve("input"));

        final List<DocRef> feeds = feedStore.list();
        logDocRefs(feeds, "feeds");

        generateSampleStatisticsData();

        // code to check that the statisticsDataSource objects are stored
        // correctly
        final List<DocRef> statisticsDataSources = statisticStoreStore.list();
        logDocRefs(statisticsDataSources, "statisticStores");

        final List<DocRef> stroomStatsStoreEntities = stroomStatsStoreStore.list();
        logDocRefs(stroomStatsStoreEntities, "stroomStatsStores");

        // Create stream processors for all feeds.
        for (final DocRef feed : feeds) {
            // Find the pipeline for this feed.
            final List<DocRef> pipelines = pipelineStore.list().stream()
                    .filter(docRef -> feed.getName()
                            .equals(docRef.getName()))
                    .collect(Collectors.toList());

            if (pipelines == null || pipelines.size() == 0) {
                LOGGER.warn("No pipeline found for feed '" + feed.getName() + "'");
            } else if (pipelines.size() > 1) {
                LOGGER.warn("More than 1 pipeline found for feed '" + feed.getName() + "'");
            } else {
                final DocRef pipeline = pipelines.get(0);

                // Create a processor for this feed.
                final QueryData criteria = new QueryData.Builder()
                        .dataSource(MetaFields.STREAM_STORE_DOC_REF)
                        .expression(new ExpressionOperator.Builder(ExpressionOperator.Op.AND)
                                .addTerm(MetaFields.FEED_NAME, ExpressionTerm.Condition.EQUALS, feed.getName())
                                .addOperator(new ExpressionOperator.Builder(ExpressionOperator.Op.OR)
                                        .addTerm(MetaFields.TYPE_NAME,
                                                ExpressionTerm.Condition.EQUALS,
                                                StreamTypeNames.RAW_EVENTS)
                                        .addTerm(MetaFields.TYPE_NAME,
                                                ExpressionTerm.Condition.EQUALS,
                                                StreamTypeNames.RAW_REFERENCE)
                                        .build())
                                .build())
                        .build();
                final Processor processor = processorService.create(pipeline, true);
                final ProcessorFilter processorFilter = processorFilterService.create(
                        processor,
                        criteria,
                        10,
                        false,
                        true);
                LOGGER.debug(processorFilter.toString());
            }
        }

//        if (shutdown) {
//            commonTestControl.shutdown();
//        }
    }

    private void checkVolumesExist() {
        final List<IndexVolume> indexVolumes = indexVolumeGroupService.getNames()
                        .stream()
                .flatMap(groupName -> indexVolumeService.find(new ExpressionCriteria()).stream())
                .collect(Collectors.toList());

        LOGGER.info("Checking available index volumes, found:\n{}",
                indexVolumes.stream()
                .map(IndexVolume::getPath)
                .collect(Collectors.joining("\n")));

        final List<FsVolume> dataVolumes = fsVolumeService.find(FindFsVolumeCriteria.matchAll()).getValues();
        LOGGER.info("Checking available data volumes, found:\n{}",
                dataVolumes.stream()
                        .map(FsVolume::getPath)
                        .collect(Collectors.joining("\n")));

        if (dataVolumes.isEmpty() || indexVolumes.isEmpty()) {
            LOGGER.error("Missing volumes, quiting");
            System.exit(1);
        }
    }

    /**
     * Generate data for testing viewing of raw/cooked data
     */
    private void createAndLoadGeneratedData(final Path inputDir) {
        int shortLoremText = 4;
        int longLoremText = 200;

        try {
            Files.createDirectories(inputDir);
            LOGGER.info("Clearing contents of {}", inputDir.toAbsolutePath().normalize());
            FileUtil.deleteContents(inputDir);
        } catch (IOException e) {
            throw new RuntimeException(LogUtil.message("Error ensuring directory {} exists",
                    inputDir.toAbsolutePath().normalize()), e);
        }

        // Data that has one record per line
        // One with long lines, one with short
        generateDataViewRawData(
                inputDir,
                1,
                "DATA_VIEWING_MULTI_LINE",
                "\n",
                shortLoremText);
        generateDataViewRawData(
                inputDir,
                2,
                "DATA_VIEWING_MULTI_LINE",
                "\n",
                longLoremText);

        // Data that is all on one massive single line
        // One with long lines, one with short
        generateDataViewRawData(
                inputDir,
                1,
                "DATA_VIEWING_SINGLE_LINE",
                "|",
                shortLoremText);
    }

    private void createIndexingProcessorFilter(
            final String pipelineName,
            final String sourceStreamType,
            final Optional<String> optFeedName) {

        // Find the pipeline for this index.
        final List<DocRef> pipelines = pipelineStore.list()
                .stream()
                .filter(docRef -> pipelineName.equals(docRef.getName()))
                .collect(Collectors.toList());

        if (pipelines == null || pipelines.size() == 0) {
            throw new RuntimeException(LogUtil.message(
                    "Expecting to find one pipeline with name [{}]", pipelineName));
        } else if (pipelines.size() > 1) {
            throw new RuntimeException(LogUtil.message(
                    "More than 1 pipeline found for index [{}]", pipelineName));
        } else {
            final DocRef pipeline = pipelines.get(0);

            final ExpressionOperator.Builder expressionBuilder = new ExpressionOperator.Builder(ExpressionOperator.Op.AND)
                    .addTerm(
                            MetaFields.TYPE_NAME,
                            ExpressionTerm.Condition.EQUALS,
                            sourceStreamType);

            optFeedName.ifPresent(feedName ->
                    expressionBuilder.addTerm(MetaFields.FEED_NAME, ExpressionTerm.Condition.EQUALS, feedName));

            // Create a processor for this index.
            final QueryData criteria = new QueryData.Builder()
                    .dataSource(MetaFields.STREAM_STORE_DOC_REF)
                    .expression(expressionBuilder.build())
                    .build();

            final Processor processor = processorService.create(pipeline, true);
            final ProcessorFilter processorFilter = processorFilterService.create(
                    processor,
                    criteria,
                    10,
                    false,
                    true);
            LOGGER.debug(processorFilter.toString());
        }
    }

    private static void logDocRefs(List<DocRef> entities, String entityTypes) {
        LOGGER.info("Listing loaded {}:", entityTypes);
        entities.stream()
                .map(DocRef::getName)
                .sorted()
                .forEach(name -> LOGGER.info("  {}", name));
    }

    public void loadDirectory(final boolean shutdown, final Path importRootDir) {
        LOGGER.info("Loading sample data for directory: " + FileUtil.getCanonicalPath(importRootDir));

        final Path configDir = importRootDir.resolve("config");
        final Path dataDir = importRootDir.resolve("input");
        final Path exampleDataDir = importRootDir.resolve("example_data");

//        createStreamAttributes();

        if (Files.exists(configDir)) {
            // Load config.
            importExportSerializer.read(configDir, null, ImportMode.IGNORE_CONFIRMATION);

//            // Enable all flags for all feeds.
//            final List<FeedDoc> feeds = feedService.find(new FindFeedCriteria());
//            for (final FeedDoc feed : feeds) {
//                feed.setStatus(FeedStatus.RECEIVE);
//                feedService.save(feed);
//            }

//            LOGGER.info("Volume count = " + commonTestControl.countEntity(VolumeEntity.TABLE_NAME));
            LOGGER.info("Feed count = " + feedStore.list().size());
//            LOGGER.info("StreamAttributeKey count = " + commonTestControl.countEntity(StreamAttributeKey.class));
            LOGGER.info("Dashboard count = " + dashboardStore.list().size());
            LOGGER.info("Pipeline count = " + pipelineStore.list().size());
            LOGGER.info("Index count = " + indexStore.list().size());
            LOGGER.info("StatisticDataSource count = " + statisticStoreStore.list().size());

        } else {
            LOGGER.info("Directory {} doesn't exist so skipping", configDir);
        }

        if (Files.exists(dataDir)) {
            // Load data.
            final DataLoader dataLoader = new DataLoader(feedProperties, streamStore);

            // We spread the received time over 10 min intervals to help test
            // repo
            // layout start 2 weeks ago.
            final long dayMs = 1000 * 60 * 60 * 24;
            final long tenMinMs = 1000 * 60 * 10;
            long startTime = System.currentTimeMillis() - (14 * dayMs);

            // Load each data item 10 times to create a reasonable amount to
            // test.
            final String feedName = "DATA_SPLITTER-EVENTS";
            for (int i = 0; i < LOAD_CYCLES; i++) {
                LOGGER.info("Loading data from {}, iteration {}", dataDir.toAbsolutePath().toString(), i);
                // Load reference data first.
                dataLoader.read(dataDir, true, startTime);
                startTime += tenMinMs;

                // Then load event data.
                dataLoader.read(dataDir, false, startTime);
                startTime += tenMinMs;

                // Load some randomly generated data.
                final String randomData = createRandomData();
                dataLoader.loadInputStream(
                        feedName,
                        "Gen data",
                        StreamUtil.stringToStream(randomData),
                        false,
                        startTime);
                startTime += tenMinMs;
            }
        } else {
            LOGGER.info("Directory {} doesn't exist so skipping", dataDir);
        }

        // Load the example data that we don't want to duplicate as is done above
        if (Files.exists(exampleDataDir)) {
            LOGGER.info("Loading example data from {}", exampleDataDir.toAbsolutePath().toString());
            // Load data.
            final DataLoader dataLoader = new DataLoader(feedProperties, streamStore);
            long startTime = System.currentTimeMillis();

            // Then load event data.
            dataLoader.read(exampleDataDir, false, startTime);
        } else {
            LOGGER.info("Directory {} doesn't exist so skipping", exampleDataDir);
        }

        // processorTaskManager.doCreateTasks();

        // // Add an index.
        // final Index index = addIndex();
        // addUserSearch(index);
        // addDictionarySearch(index);
    }

    private void loadStatsData(final DataLoader dataLoader,
                               final String feedName,
                               final int iterations,
                               final Instant startTime,
                               final BiFunction<Integer, Instant, String> dataGenerationFunction) {
        try {
            LOGGER.info("Generating statistics test data for feed {}", feedName);
            dataLoader.loadInputStream(
                    feedName,
                    "Auto generated statistics data",
                    StreamUtil.stringToStream(dataGenerationFunction.apply(iterations, startTime)),
                    false,
                    startTime.toEpochMilli());
        } catch (final RuntimeException e) {
            LOGGER.error("Feed {} does not exist so cannot load the sample statistics data", feedName, e);
        }
    }

    /**
     * Generates some sample statistics data in two feeds. If the feed doesn't
     * exist it will fail silently
     */
    private void generateSampleStatisticsData() {
        final DataLoader dataLoader = new DataLoader(feedProperties, streamStore);
        final long startTime = System.currentTimeMillis();

        //keep the big and small feeds apart in terms of their event times
        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant startOfAWeekAgo = startOfToday.minus(7, ChronoUnit.DAYS);

        loadStatsData(
                dataLoader,
                STATS_COUNT_FEED_LARGE_NAME,
                STATS_ITERATIONS_LARGE,
                startOfAWeekAgo,
                GenerateSampleStatisticsData::generateCountData);

        loadStatsData(
                dataLoader,
                STATS_COUNT_FEED_SMALL_NAME,
                STATS_ITERATIONS_SMALL,
                startOfToday,
                GenerateSampleStatisticsData::generateCountData);

        loadStatsData(
                dataLoader,
                STATS_VALUE_FEED_LARGE_NAME,
                STATS_ITERATIONS_LARGE,
                startOfAWeekAgo,
                GenerateSampleStatisticsData::generateValueData);

        loadStatsData(
                dataLoader,
                STATS_VALUE_FEED_SMALL_NAME,
                STATS_ITERATIONS_SMALL,
                startOfToday,
                GenerateSampleStatisticsData::generateValueData);

        try {
            final String sampleData = new String(Files.readAllBytes(Paths.get(STATS_COUNT_API_DATA_FILE)));
            dataLoader.loadInputStream(
                    STATS_COUNT_API_FEED_NAME,
                    "Sample statistics count data for export to API",
                    StreamUtil.stringToStream(sampleData),
                    false,
                    startTime);
        } catch (final RuntimeException | IOException e) {
            LOGGER.warn("Feed {} does not exist so cannot load the sample count for export to API statistics data.",
                    STATS_COUNT_API_FEED_NAME);
        }
    }

    private String createRandomData() {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy,HH:mm:ss");
        final ZonedDateTime refDateTime = ZonedDateTime.of(
                2010, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        final StringBuilder sb = new StringBuilder();
        sb.append("Date,Time,FileNo,LineNo,User,Message\n");

        for (int i = 0; i < 1000; i++) {
            final ZonedDateTime dateTime = refDateTime.plusSeconds((long) (Math.random() * 10000));
            sb.append(formatter.format(dateTime));
            sb.append(",");
            sb.append(createNum(4));
            sb.append(",");
            sb.append(createNum(10));
            sb.append(",user");
            sb.append(createNum(10));
            sb.append(",Some message ");
            sb.append(createNum(10));
            sb.append("\n");
        }
        return sb.toString();
    }

    private Path makeInputFilePath(final Path dir, final int index, final String feedName) {
        return dir.resolve(feedName + "~" + index + ".in");
    }

    private void generateDataViewRawData(final Path dir,
                                         final int fileNo,
                                         final String feedName,
                                         final String recordSeparator,
                                         final int loremWordCount) {
        final Path file = makeInputFilePath(dir, fileNo, feedName);
        LOGGER.info("Generating file {}", file.toAbsolutePath().normalize().toString());
        DataGenerator.buildDefinition()
                .addFieldDefinition(DataGenerator.uuidField(
                        "uuid"))
                .addFieldDefinition(DataGenerator.fakerField(
                        "firstName",
                        faker -> faker.name().firstName()))
                .addFieldDefinition(DataGenerator.fakerField(
                        "surname",
                        faker -> faker.name().lastName()))
                .addFieldDefinition(DataGenerator.fakerField(
                        "username",
                        faker -> faker.name().username()))
                .addFieldDefinition(DataGenerator.fakerField(
                        "bloodGroup",
                        faker -> faker.name().bloodGroup()))
                .addFieldDefinition(DataGenerator.randomEmoticonEmojiField(
                        "emotionalState"))
                .addFieldDefinition(DataGenerator.fakerField(
                        "address",
                        faker -> faker.address().fullAddress()))
                .addFieldDefinition(DataGenerator.fakerField(
                        "company",
                        faker -> faker.company().name()))
                .addFieldDefinition(DataGenerator.fakerField(
                        "companyLogo",
                        faker -> faker.company().logo()))
                .addFieldDefinition(DataGenerator.fakerField(
                        "lorum",
                        faker -> String.join(" ", faker.lorem().words(loremWordCount))))
                .setDataWriter(FlatDataWriterBuilder.builder()
                        .delimitedBy(",")
                        .enclosedBy("\"")
                        .outputHeaderRow(true)
                        .build())
                .consumedBy(DataGenerator.getFileOutputConsumer(file, recordSeparator))
                .rowCount(5_000)
                .withRandomSeed(fileNo)
                .generate();
    }

    private String createNum(final int max) {
        return String.valueOf((int) (Math.random() * max) + 1);
    }

    // private Folder get(String name) {
    // Folder parentGroup = null;
    // Folder folder = null;
    //
    // String[] parts = name.split("/");
    // for (String part : parts) {
    // parentGroup = folder;
    // folder = folderService.loadByName(parentGroup, part);
    // }
    // return folder;
    // }
    //
    // private Index addIndex() {
    // try {
    // final Folder folder = get("Indexes/Example index");
    // final Pipeline indexTranslation = findTranslation("Example index");
    // return setupIndex(folder, "Example index", indexTranslation);
    //
    // } catch (final IOException e) {
    // throw new RuntimeException(e.getMessage(), e);
    // }
    // }
    //
    // private Pipeline findTranslation(final String name) {
    // final FindPipelineCriteria findTranslationCriteria = new
    // FindPipelineCriteria();
    // findTranslationCriteria.setName(name);
    // final BaseResultList<Pipeline> list = pipelineStore
    // .find(findTranslationCriteria);
    // if (list != null && list.size() > 0) {
    // return list.getFirst();
    // }
    //
    // throw new RuntimeException("No translation found with name: " + name);
    // }
    //
    // private XSLT findXSLT(final String name) {
    // final FindXSLTCriteria findXSLTCriteria = new FindXSLTCriteria();
    // findXSLTCriteria.setName(name);
    // final BaseResultList<XSLT> list = xsltStore.find(findXSLTCriteria);
    // if (list != null && list.size() > 0) {
    // return list.getFirst();
    // }
    //
    // throw new RuntimeException("No translation found with name: " + name);
    // }
    //
    // private Index setupIndex(final Folder folder,
    // final String indexName, final Pipeline indexTranslation)
    // throws IOException {
    // Index index = new Index();
    // index.setFolder(folder);
    // index.setName(indexName);
    //
    // index = indexStore.save(index);
    //
    // return index;
    // }
    //
    // private void addUserSearch(final Index index) {
    // final Folder folder = get(SEARCH + "/Search Examples");
    // final XSLT resultXSLT = findXSLT("Search Result Table - Show XML");
    //
    // final SearchExpressionTerm content1 = new SearchExpressionTerm();
    // content1.setField("UserId");
    // content1.setValue("userone");
    // final SearchExpressionOperator andOperator = new
    // SearchExpressionOperator(
    // true);
    // andOperator.addChild(content1);
    //
    // // FIXME : Set result pipeline.
    // final Search expression = new Search(index, null, andOperator);
    // expression.setName("User search");
    // expression.setFolder(folder);
    // searchExpressionService.save(expression);
    //
    // final DictionaryDocument dictionary = new Dictionary();
    // dictionary.setName("User list");
    // dictionary.setWords("userone\nuser1");
    // }
    //
    // private void addDictionarySearch(final Index index) {
    // final Folder folder = get(SEARCH + "/Search Examples");
    // final XSLT resultXSLT = findXSLT("Search Result Table - Show XML");
    //
    // final DictionaryDocument dictionary = new Dictionary();
    // dictionary.setName("User list");
    // dictionary.setWords("userone\nuser1");
    // dictionary.setFolder(folder);
    //
    // dictionaryStore.save(dictionary);
    //
    // final SearchExpressionTerm content1 = new SearchExpressionTerm();
    // content1.setField("UserId");
    // content1.setOperator(Operator.IN_DICTIONARY);
    // content1.setValue("User list");
    // final SearchExpressionOperator andOperator = new
    // SearchExpressionOperator(
    // true);
    // andOperator.addChild(content1);
    //
    // // FIXME : Set result pipeline.
    // final Search expression = new Search(index, null, andOperator);
    // expression.setName("Dictionary search");
    // expression.setFolder(folder);
    //
    // searchExpressionService.save(expression);
    // }
}
