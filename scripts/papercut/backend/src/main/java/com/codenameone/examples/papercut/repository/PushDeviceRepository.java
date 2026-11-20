/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.papercut.domain.PushDevice;

import java.util.List;

/// The devices each account is sent notifications on.
@Repository(entity = PushDevice.class, key = String.class)
public interface PushDeviceRepository extends CrudRepository<PushDevice, String> {
    List<PushDevice> findByUsername(String username);

    int deleteByUsername(String username);

    int deleteByIdAndUsername(String id, String username);
}
