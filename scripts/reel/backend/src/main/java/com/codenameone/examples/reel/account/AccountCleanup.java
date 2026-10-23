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
package com.codenameone.examples.reel.account;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.domain.Household;
import com.codenameone.examples.reel.repository.AnswerRepository;
import com.codenameone.examples.reel.repository.CertificateRepository;
import com.codenameone.examples.reel.repository.DeviceRepository;
import com.codenameone.examples.reel.repository.DownloadRepository;
import com.codenameone.examples.reel.repository.HouseholdRepository;
import com.codenameone.examples.reel.repository.ListEntryRepository;
import com.codenameone.examples.reel.repository.PlaybackRepository;
import com.codenameone.examples.reel.repository.ProgressRepository;
import com.codenameone.examples.reel.repository.PushDeviceRepository;
import com.codenameone.examples.reel.repository.RentalRepository;
import com.codenameone.examples.reel.repository.SharingAlertRepository;
import com.codenameone.examples.reel.repository.SubscriptionRepository;
import com.codenameone.examples.reel.repository.TitleRatingRepository;

import java.util.List;

/// Deletes what a closed account leaves in the viewing tables, inside the
/// closing transaction ([Accounts#close]). Invoices are not touched: they are the
/// service's accounts, and stay under the retired name. Titles the account
/// published stay too, unpublished by the studio when their creator is gone.
@Component
public class AccountCleanup {
    private final HouseholdRepository households;
    private final ListEntryRepository lists;
    private final DeviceRepository devices;
    private final ProgressRepository progress;
    private final TitleRatingRepository ratings;
    private final DownloadRepository downloads;
    private final AnswerRepository answers;
    private final PlaybackRepository playbacks;
    private final CertificateRepository certificates;
    private final SharingAlertRepository alerts;
    private final SubscriptionRepository subscriptions;
    private final RentalRepository rentals;
    private final PushDeviceRepository pushes;

    public AccountCleanup(HouseholdRepository households, ListEntryRepository lists,
            DeviceRepository devices, ProgressRepository progress, TitleRatingRepository ratings,
            DownloadRepository downloads, AnswerRepository answers, PlaybackRepository playbacks,
            CertificateRepository certificates, SharingAlertRepository alerts,
            SubscriptionRepository subscriptions, RentalRepository rentals,
            PushDeviceRepository pushes) {
        this.households = households;
        this.lists = lists;
        this.devices = devices;
        this.progress = progress;
        this.ratings = ratings;
        this.downloads = downloads;
        this.answers = answers;
        this.playbacks = playbacks;
        this.certificates = certificates;
        this.alerts = alerts;
        this.subscriptions = subscriptions;
        this.rentals = rentals;
        this.pushes = pushes;
    }

    public void removeEverythingOf(String username) {
        List<Household> mine = households.findByUsernameOrderByCreatedAtAsc(username);
        for (int iter = 0; iter < mine.size(); iter++) {
            lists.deleteByProfileId(mine.get(iter).getId());
        }
        households.deleteByUsername(username);
        devices.deleteByUsername(username);
        progress.deleteByUsername(username);
        ratings.deleteByUsername(username);
        downloads.deleteByUsername(username);
        answers.deleteByUsername(username);
        playbacks.deleteByUsername(username);
        certificates.deleteByUsername(username);
        alerts.deleteByUsername(username);
        rentals.deleteByUsername(username);
        pushes.deleteByUsername(username);
        if (subscriptions.findById(username) != null) {
            subscriptions.deleteById(username);
        }
    }
}
