/*
 * Copyright 2016 Crown Copyright
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
 */

package stroom.data.client.presenter;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;
import stroom.docref.DocRef;
import stroom.docref.SharedObject;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.processor.shared.FindProcessorTaskCriteria;
import stroom.processor.shared.ProcessorTaskSummaryRow;

public class ProcessorTaskPresenter extends MyPresenterWidget<ProcessorTaskPresenter.StreamTaskView>
        implements HasDocumentRead<SharedObject> {
    public static final String STREAM_TASK_LIST = "STREAM_TASK_LIST";
    public static final String STREAM_TASK_SUMMARY = "STREAM_TASK_SUMMARY";
    private final ProcessorTaskSummaryPresenter processorTaskSummaryPresenter;
    private final ProcessorTaskListPresenter processorTaskListPresenter;

    @Inject
    public ProcessorTaskPresenter(final EventBus eventBus, final StreamTaskView view,
                                  final ProcessorTaskSummaryPresenter processorTaskSummaryPresenter,
                                  final ProcessorTaskListPresenter processorTaskListPresenter) {
        super(eventBus, view);
        this.processorTaskSummaryPresenter = processorTaskSummaryPresenter;
        this.processorTaskListPresenter = processorTaskListPresenter;

        setInSlot(STREAM_TASK_SUMMARY, processorTaskSummaryPresenter);
        setInSlot(STREAM_TASK_LIST, processorTaskListPresenter);
    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(processorTaskSummaryPresenter.getSelectionModel().addSelectionHandler(event -> {
            // Clear the task list.
            processorTaskListPresenter.clear();

            final ProcessorTaskSummaryRow row = processorTaskSummaryPresenter.getSelectionModel().getSelected();

            if (row != null) {
                final FindProcessorTaskCriteria findProcessorTaskCriteria = processorTaskListPresenter.getCriteria();

                final String pipelineUuid = row.getPipeline();

                findProcessorTaskCriteria.obtainPipelineUuidCriteria().clear();
                if (pipelineUuid != null) {
                    findProcessorTaskCriteria.obtainPipelineUuidCriteria().setString(pipelineUuid);
                }

//                findStreamTaskCriteria.obtainFeedNameSet().clear();
//                findStreamTaskCriteria.obtainFeedNameSet().add(row.getLabel().get(FindProcessorTaskCriteria.SUMMARY_POS_FEED));

                findProcessorTaskCriteria.obtainTaskStatusSet().clear();
                findProcessorTaskCriteria.obtainTaskStatusSet().add(row.getStatus());

                processorTaskListPresenter.getDataProvider().refresh();
            }
        }));
    }

    @Override
    public void read(final DocRef docRef, final SharedObject entity) {
        processorTaskSummaryPresenter.read(docRef, entity);
        processorTaskListPresenter.read(docRef, entity);
    }

    public interface StreamTaskView extends View {
    }
}