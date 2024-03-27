/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.modelconfig.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileLoaderTest {

    @Test
    public void testFormatClassPath() {
        Assertions.assertEquals("anydir", FileLoader.formatClassPath("src/test/resources/anydir"));
        Assertions.assertEquals("anydir/configs", FileLoader.formatClassPath("src/test/resources/anydir/configs"));
        Assertions.assertEquals("src/test/resourc", FileLoader.formatClassPath("src/test/resourc"));
        Assertions.assertEquals("", FileLoader.formatClassPath("src/test/resources/"));
        Assertions.assertEquals("", FileLoader.formatClassPath("src/test/resources"));
        Assertions.assertEquals("anydir/configs", FileLoader.formatClassPath("src/test/resourcesanydir/configs"));
    }
}
