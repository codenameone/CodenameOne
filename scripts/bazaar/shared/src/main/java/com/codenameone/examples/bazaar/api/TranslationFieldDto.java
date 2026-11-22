/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.Mapped;

/// One row of a translation sheet: a translatable field of a product (or of
/// one of its options, pictures or text metafields), a collection or a page,
/// with its original beside the translation in one locale.
///
/// @param kind what the field belongs to, as in `TranslationDto`
/// @param refId the id of that thing
/// @param field the field's name within the kind
/// @param label what the editor calls the field
/// @param original the text as the merchant wrote it
/// @param value the translation, empty when there is none
/// @param status `MISSING`, `OUTDATED` (the original changed after it was
///     translated) or `DONE`
@Mapped
public record TranslationFieldDto(
        String kind,
        String refId,
        String field,
        String label,
        String original,
        String value,
        String status) {
}
