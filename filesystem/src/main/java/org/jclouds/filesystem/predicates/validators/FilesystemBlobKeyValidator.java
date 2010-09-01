/**
 *
 * Copyright (C) 2010 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.jclouds.filesystem.predicates.validators;

import com.google.inject.Singleton;
import java.io.File;
import org.jclouds.predicates.Validator;

/**
 * Validates name for filesystem container blob keys
 *
 * @see org.jclouds.rest.InputParamValidator
 * @see org.jclouds.predicates.Validator
 *
 * @author Alfredo "Rainbowbreeze" Morresi
 */
@Singleton
public class FilesystemBlobKeyValidator extends Validator<String> {

    @Override
    public void validate(String name) throws IllegalArgumentException {
        //blob key cannot be null or empty
        if (name == null || name.length() < 1)
            throw new IllegalArgumentException("Blob key can't be null or empty");

        //blobkey cannot start with / (or \ in Windows) character
        if (name.startsWith(File.separator))
            throw new IllegalArgumentException(String.format(
                    "Blob key '%s' cannot start with character %s", name, File.separator));

        //blobkey cannot end with / (or \ in Windows) character
        if (name.endsWith(File.separator))
            throw new IllegalArgumentException(String.format(
                    "Blob key '%s' cannot end with character %s", name, File.separator));
    }

}
