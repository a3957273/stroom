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

package stroom.processor.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import stroom.util.shared.Expander;

import java.util.Objects;

@JsonPropertyOrder({"processor", "expander"})
@JsonInclude(Include.NON_DEFAULT)
public class ProcessorRow extends ProcessorListRow {
    private Processor processor;
    private Expander expander;

    public ProcessorRow() {
        // Default constructor necessary for GWT serialisation.
    }

    public ProcessorRow(final Expander expander, final Processor processor) {
        this.processor = processor;
        this.expander = expander;
    }

    public Processor getProcessor() {
        return processor;
    }

    public void setProcessor(final Processor processor) {
        this.processor = processor;
    }

    @Override
    public Expander getExpander() {
        return expander;
    }

    public void setExpander(final Expander expander) {
        this.expander = expander;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ProcessorRow that = (ProcessorRow) o;
        return Objects.equals(processor, that.processor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processor);
    }

    @Override
    public String toString() {
        return "ProcessorRow{" +
                "processor=" + processor +
                ", expander=" + expander +
                '}';
    }
}
