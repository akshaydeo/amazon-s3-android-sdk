package com.amazon.s3.internal;

/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.amazon.s3.model.ProgressEvent;
import com.amazon.s3.model.ProgressListener;


/**
 * Simple InputStream wrapper that occasionally notifies a progress listener
 * about the number of bytes transfered.
 */
public class ProgressReportingInputStream extends FilterInputStream {

    /** The threshold of bytes between notifications. */
    private static final int NOTIFICATION_THRESHOLD = 8 * Constants.KB;

    /** The listener to notify. */
    private final ProgressListener listener;

    /** The number of bytes read that the listener hasn't been notified about yet. */
    private int unnotifiedByteCount;

    /** True if this stream should fire a completed progress event when the stream runs out. */
    private boolean fireCompletedEvent;


    /**
     * Creates a new progress reporting input stream that simply wraps the
     * specified input stream and notifies the specified listener occasionally
     * about the number of bytes transfered.
     *
     * @param in
     *            The input stream to wrap.
     * @param listener
     *            The listener to notify about progress.
     */
    public ProgressReportingInputStream(final InputStream in, final ProgressListener listener) {
        super(in);
        this.listener = listener;
    }

    /**
     * Sets whether this input stream should fire an event with code
     * {@link ProgressEvent#COMPLETED_EVENT_CODE} when this stream runs out of
     * data. By default, completed events are not fired by this stream.
     *
     * @param fireCompletedEvent
     *            Whether this input stream should fire an event to indicate
     *            that the stream has been fully read.
     */
    public void setFireCompletedEvent(boolean fireCompletedEvent) {
        this.fireCompletedEvent = fireCompletedEvent;
    }

    /**
     * Returns whether this input stream should fire an event with code
     * {@link ProgressEvent#COMPLETED_EVENT_CODE} when this stream runs out of
     * data. By default, completed events are not fired by this stream.
     *
     * @return Whether this input stream should fire an event to indicate that
     *         the stream has been fully read.
     */
    public boolean getFireCompletedEvent() {
        return fireCompletedEvent;
    }

    @Override
    public int read() throws IOException {
        int data = super.read();
        if (data == -1) notifyCompleted();
        if (data != -1) notify(1);
        return data;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        if (bytesRead == -1) notifyCompleted();
        if (bytesRead != -1) notify(bytesRead);
        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        if (unnotifiedByteCount > 0) {
            listener.progressChanged(new ProgressEvent(unnotifiedByteCount));
            unnotifiedByteCount = 0;
        }
        super.close();
    }

    private void notifyCompleted() {
        if (fireCompletedEvent == false) return;

        ProgressEvent event = new ProgressEvent(unnotifiedByteCount);
        event.setEventCode(ProgressEvent.COMPLETED_EVENT_CODE);
        unnotifiedByteCount = 0;
        listener.progressChanged(event);
    }

    private void notify(int bytesRead) {
        unnotifiedByteCount += bytesRead;
        if (unnotifiedByteCount >= NOTIFICATION_THRESHOLD) {
            listener.progressChanged(new ProgressEvent(unnotifiedByteCount));
            unnotifiedByteCount = 0;
        }
    }
}
