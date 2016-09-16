/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.documentsui.dirlist;

import static com.android.documentsui.base.DocumentInfo.getCursorInt;
import static com.android.documentsui.base.DocumentInfo.getCursorString;

import android.database.Cursor;
import android.provider.DocumentsContract.Document;
import android.util.Log;

import com.android.documentsui.MenuManager;
import com.android.documentsui.MimePredicate;

import java.util.function.Function;

/**
 * A class that holds metadata
 */
class SelectionMetadata implements MenuManager.SelectionDetails, MultiSelectManager.ItemCallback {

    private static final String TAG = "SelectionMetadata";

    private final MultiSelectManager mSelectionMgr;
    private final Function<String, Cursor> mDocFinder;

    // Partial files are files that haven't been fully downloaded.
    private int mPartialCount = 0;
    private int mDirectoryCount = 0;
    private int mWritableDirectoryCount = 0;
    private int mNoDeleteCount = 0;
    private int mNoRenameCount = 0;

    SelectionMetadata(
            MultiSelectManager selectionMgr, Function<String, Cursor> docFinder) {
        mSelectionMgr = selectionMgr;
        mDocFinder = docFinder;
    }

    @Override
    public void onItemStateChanged(String modelId, boolean selected) {
        final Cursor cursor = mDocFinder.apply(modelId);
        if (cursor == null) {
            Log.w(TAG, "Model returned null cursor for document: " + modelId
                    + ". Ignoring state changed event.");
            return;
        }

        final String mimeType = getCursorString(cursor, Document.COLUMN_MIME_TYPE);
        if (MimePredicate.isDirectoryType(mimeType)) {
            mDirectoryCount += selected ? 1 : -1;
        }

        final int docFlags = getCursorInt(cursor, Document.COLUMN_FLAGS);
        if ((docFlags & Document.FLAG_PARTIAL) != 0) {
            mPartialCount += selected ? 1 : -1;
        }
        if ((docFlags & Document.FLAG_DIR_SUPPORTS_CREATE) != 0) {
            mWritableDirectoryCount += selected ? 1 : -1;
        }
        if ((docFlags & Document.FLAG_SUPPORTS_DELETE) == 0) {
            mNoDeleteCount += selected ? 1 : -1;
        }
        if ((docFlags & Document.FLAG_SUPPORTS_RENAME) == 0) {
            mNoRenameCount += selected ? 1 : -1;
        }
    }

    @Override
    public boolean containsDirectories() {
        return mDirectoryCount > 0;
    }

    @Override
    public boolean containsPartialFiles() {
        return mPartialCount > 0;
    }

    @Override
    public boolean canDelete() {
        return mNoDeleteCount == 0;
    }

    @Override
    public boolean canRename() {
        return mNoRenameCount == 0 && mSelectionMgr.getSelection().size() == 1;
    }

    @Override
    public boolean canPasteInto() {
        return mDirectoryCount == 1 && mWritableDirectoryCount == 1
                && mSelectionMgr.getSelection().size() == 1;
    }
}
