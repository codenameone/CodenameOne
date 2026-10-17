/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

import com.codename1.impl.JdkHost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/// java.util.ServiceLoader over the services files in `res/META-INF/services`:
/// the order of the providers, what a file may hold besides names, when a
/// provider is created and how often. Then the loader over a registry, as a
/// built application has one.
public final class ServiceLoaderChecks {
    /// The providers created, in order; the providers add themselves.
    static final List<String> CREATED = new ArrayList<String>();

    private ServiceLoaderChecks() {
    }

    /// What the build generates for an application: the providers of each
    /// service in order, made without reflection.
    private static class Registry implements JdkHost.Registry {
        @Override
        public String[] providers(String service) {
            if (!ServiceLoaderGreeter.class.getName().equals(service)) {
                return null;
            }
            return new String[] {ServiceLoaderGerman.class.getName(), "java.lang.String", "no.such.Provider",
                ServiceLoaderEnglish.class.getName()};
        }

        @Override
        public Object newInstance(String className) {
            if (ServiceLoaderGerman.class.getName().equals(className)) {
                return new ServiceLoaderGerman();
            }
            if (ServiceLoaderEnglish.class.getName().equals(className)) {
                return new ServiceLoaderEnglish();
            }
            return "java.lang.String".equals(className) ? "not a greeter" : null;
        }

        @Override
        public Class<?> type(String className) {
            if (ServiceLoaderGerman.class.getName().equals(className)) {
                return ServiceLoaderGerman.class;
            }
            if (ServiceLoaderEnglish.class.getName().equals(className)) {
                return ServiceLoaderEnglish.class;
            }
            return "java.lang.String".equals(className) ? String.class : null;
        }
    }

    private static List<String> created() {
        List<String> out = new ArrayList<String>(CREATED);
        CREATED.clear();
        return out;
    }

    /// One step of a walk over a loader, as text.
    private static String step(Iterator<ServiceLoaderGreeter> it, String what) {
        String answer;
        if ("hasNext".equals(what)) {
            answer = String.valueOf(it.hasNext());
        } else {
            try {
                answer = it.next().greet();
            } catch (NoSuchElementException e) {
                answer = "NoSuchElementException";
            }
        }
        return what + "=" + answer + " created " + created();
    }

    /// A check's body run with no creation recorded before it.
    private static Bulk.Body fresh(final Bulk.Body body) {
        return () -> {
            CREATED.clear();
            body.run();
        };
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("ServiceLoader.theProvidersComeInFileOrderWithoutCommentsBlanksOrRepeats", fresh(() -> {
            List<String> mine = new ArrayList<String>();
            for (ServiceLoaderGreeter g : ServiceLoader.load(ServiceLoaderGreeter.class)) {
                mine.add(g.getClass().getName() + "=" + g.greet());
            }
            Bulk.p("providers", mine);
            Bulk.p("created", created());
        }));
        Bulk.check("ServiceLoader.aProviderIsCreatedWhenTheIterationReachesItAndOncePerLoader", fresh(() -> {
            String[] walk = {"next", "hasNext", "hasNext", "next", "next", "hasNext", "next"};
            ServiceLoader<ServiceLoaderGreeter> mine = ServiceLoader.load(ServiceLoaderGreeter.class);
            Bulk.p("loadingCreates", created());
            Iterator<ServiceLoaderGreeter> b = mine.iterator();
            for (int i = 0; i < walk.length; i++) {
                Bulk.p("step" + i, step(b, walk[i]));
            }
            // A second iteration answers the same instances and creates nothing.
            ServiceLoaderGreeter first = mine.iterator().next();
            Bulk.p("sameInstance", first == mine.iterator().next());
            Bulk.p("createdAfter", created());
        }));
        Bulk.check("ServiceLoader.twoIterationsShareWhatEitherCreated", fresh(() -> {
            ServiceLoader<ServiceLoaderGreeter> mine = ServiceLoader.load(ServiceLoaderGreeter.class);
            Iterator<ServiceLoaderGreeter> b1 = mine.iterator();
            Iterator<ServiceLoaderGreeter> b2 = mine.iterator();
            Bulk.p("1", step(b1, "next"));
            Bulk.p("2", step(b2, "next"));
            Bulk.p("3", step(b2, "next"));
            Bulk.p("4", step(b1, "next"));
            Bulk.p("5", step(b1, "next"));
            Bulk.p("6", step(b2, "next"));
            Bulk.p("7", step(b2, "hasNext"));
            Bulk.p("8", step(b1, "next"));
        }));
        Bulk.check("ServiceLoader.reloadCreatesTheProvidersAgain", fresh(() -> {
            ServiceLoader<ServiceLoaderGreeter> mine = ServiceLoader.load(ServiceLoaderGreeter.class);
            ServiceLoaderGreeter myFirst = mine.iterator().next();
            created();
            mine.reload();
            Bulk.p("reloadCreates", created());
            Bulk.p("sameAfterReload", myFirst == mine.iterator().next());
            Bulk.p("created", created());
        }));
        Bulk.check("ServiceLoader.anIteratorFromBeforeAReloadRefusesToGoOn", fresh(() -> {
            ServiceLoader<ServiceLoaderGreeter> mine = ServiceLoader.load(ServiceLoaderGreeter.class);
            Iterator<ServiceLoaderGreeter> before = mine.iterator();
            before.next();
            mine.reload();
            Bulk.pThrown("hasNext", java.util.ConcurrentModificationException.class, () -> before.hasNext());
            Bulk.pThrown("next", java.util.ConcurrentModificationException.class, () -> before.next());
            Bulk.p("freshHasNext", mine.iterator().hasNext());
        }));
        Bulk.check("ServiceLoader.aServiceWithNoProviderHasNone", fresh(() -> {
            Bulk.p("empty", ServiceLoader.load(ServiceLoaderEmpty.class).iterator().hasNext());
            Bulk.p("unlisted", ServiceLoader.load(ServiceLoaderUnlisted.class).iterator().hasNext());
            Bulk.p("unlistedFindFirst", ServiceLoader.load(ServiceLoaderUnlisted.class).findFirst().isPresent());
            Bulk.p("emptyFindFirst", ServiceLoader.load(ServiceLoaderEmpty.class).findFirst().isPresent());
            Bulk.p("emptyStream", ServiceLoader.load(ServiceLoaderEmpty.class).stream().count());
            // What the application ships is never "installed in the platform".
            ServiceLoader<ServiceLoaderGreeter> installed = ServiceLoader.loadInstalled(ServiceLoaderGreeter.class);
            Bulk.p("installed", installed.iterator().hasNext());
            installed.reload();
            Bulk.p("installedFindFirst", installed.findFirst().isPresent());
            Bulk.p("created", created());
        }));
        Bulk.check("ServiceLoader.theOtherWaysToLoadAnswerTheSameProviders", fresh(() -> {
            ServiceLoader<ServiceLoaderGreeter> byLoader = ServiceLoader.load(ServiceLoaderGreeter.class,
                    ServiceLoaderChecks.class.getClassLoader());
            Bulk.p("byLoader", byLoader.iterator().next().greet());
            Bulk.p("nullLoader", ServiceLoader.load(ServiceLoaderGreeter.class, null).iterator().next().greet());
            Bulk.p("toString", ServiceLoader.load(ServiceLoaderGreeter.class).toString());
            Bulk.pThrown("nullService", NullPointerException.class, () -> ServiceLoader.load((Class<ServiceLoaderGreeter>) null));
        }));
        Bulk.check("ServiceLoader.findFirstCreatesOnlyTheFirstAndForEachTheRest", fresh(() -> {
            ServiceLoader<ServiceLoaderGreeter> mine = ServiceLoader.load(ServiceLoaderGreeter.class);
            Optional<ServiceLoaderGreeter> first = mine.findFirst();
            Bulk.p("present", first.isPresent());
            Bulk.p("greet", first.get().greet());
            Bulk.p("created", created());
            Bulk.p("sameFirst", first.get() == mine.findFirst().get());
            final List<String> said = new ArrayList<String>();
            mine.forEach(g -> said.add(g.greet()));
            Bulk.p("said", said);
            Bulk.p("createdByForEach", created());
            Bulk.pThrown("forEachNull", NullPointerException.class, () -> mine.forEach(null));
            Bulk.pThrown("remove", UnsupportedOperationException.class, () -> mine.iterator().remove());
        }));
        Bulk.check("ServiceLoader.aStreamCreatesNothingUntilAskedAndThenAnewEachTime", fresh(() -> {
            ServiceLoader<ServiceLoaderGreeter> mine = ServiceLoader.load(ServiceLoaderGreeter.class);
            List<ServiceLoader.Provider<ServiceLoaderGreeter>> providers = mine.stream().collect(Collectors.toList());
            Bulk.p("size", providers.size());
            Bulk.p("created", created());
            Bulk.p("typeIsFrench", providers.get(1).type() == ServiceLoaderFrench.class);
            ServiceLoaderGreeter one = providers.get(1).get();
            ServiceLoaderGreeter two = providers.get(1).get();
            Bulk.p("greet", one.greet());
            Bulk.p("sameInstance", one == two);
            Bulk.p("createdByGet", created());
        }));
        Bulk.cn1Only("ServiceLoader.aBuiltApplicationHasTheProvidersItsRegistryListsAndNoOthers", fresh(() -> {
            JdkHost.setRegistry(new Registry());
            ServiceLoader<ServiceLoaderGreeter> mine = ServiceLoader.load(ServiceLoaderGreeter.class);
            Bulk.expectEquals(Arrays.asList(), created(), "loading creates nothing");
            List<String> said = new ArrayList<String>();
            for (ServiceLoaderGreeter g : mine) {
                said.add(g.greet());
            }
            // The registry's order, not the file's; what is no ServiceLoaderGreeter, or is
            // not there, is left out rather than handed over under the wrong type.
            Bulk.expectEquals(Arrays.asList("hallo", "hello"), said, "said");
            Bulk.expectEquals(Arrays.asList("German", "English"), created(), "created");
            Bulk.expectEquals(Long.valueOf(2), Long.valueOf(mine.stream().count()), "stream count");
            Bulk.expect(mine.stream().findFirst().get().type() == ServiceLoaderGerman.class, "first type is ServiceLoaderGerman");

            // A service the registry does not know has no provider, though a
            // file lists some: the registry is the whole answer.
            Bulk.expect(!ServiceLoader.load(ServiceLoaderEmpty.class).iterator().hasNext(), "Empty has none");
            JdkHost.setRegistry(new Registry() {
                @Override
                public String[] providers(String service) {
                    return null;
                }
            });
            Bulk.expect(!ServiceLoader.load(ServiceLoaderGreeter.class).iterator().hasNext(), "an empty registry has none");
            Bulk.expectEquals(Arrays.asList(), created(), "created at the end");
        }));
    }
}
