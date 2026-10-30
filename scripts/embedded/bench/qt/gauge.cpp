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

// The benchmark gauge in Qt 6 Widgets, drawn with QPainter (Qt's software
// raster engine), on the offscreen platform plugin -- the same path as
// linuxfb on a board without a GPU.
#include <QApplication>
#include <QPainter>
#include <QTimer>
#include <QWidget>
#include <cmath>
extern "C" {
#include "../common.h"
}

class Gauge : public QWidget {
public:
    double angle = 0;
    long frames = 0;
    bool first = true;
protected:
    void paintEvent(QPaintEvent*) override {
        frames++;
        QPainter g(this);
        g.setRenderHint(QPainter::Antialiasing, true);
        int w = width(), h = height(), cx = w / 2, cy = h / 2;
        int r = std::min(w, h) / 2 - 10;
        g.fillRect(0, 0, w, h, QColor(0x10, 0x18, 0x20));
        g.setPen(Qt::NoPen);
        g.setBrush(QColor(0x2a, 0x3a, 0x4a));
        g.drawEllipse(cx - r, cy - r, r * 2, r * 2);
        int sweep = (int) ((std::sin(angle) + 1) * 135);
        g.setBrush(QColor(0x33, 0xaa, 0xff));
        g.drawPie(cx - r, cy - r, r * 2, r * 2, (225 - sweep) * 16, sweep * 16);
        g.setBrush(QColor(0x10, 0x18, 0x20));
        g.drawEllipse(cx - r + 24, cy - r + 24, r * 2 - 48, r * 2 - 48);
        double a = (225 - sweep) * M_PI / 180.0;
        g.setPen(QPen(Qt::white, 1));
        g.drawLine(cx, cy, cx + (int) (std::cos(a) * (r - 30)), cy - (int) (std::sin(a) * (r - 30)));
        QFont f = g.font();
        f.setBold(true);
        f.setPixelSize(28);
        g.setFont(f);
        QString v = QString::number(sweep * 100 / 270) + " %";
        g.drawText(QRect(0, cy + r / 3 - 28, w, 40), Qt::AlignHCenter, v);
        if (first) {
            first = false;
            bench_first_frame();
        }
    }
};

int main(int argc, char** argv) {
    QApplication app(argc, argv);
    Gauge gauge;
    gauge.resize(800, 480);
    gauge.show();
    QTimer tick;
    QObject::connect(&tick, &QTimer::timeout, [&] { gauge.angle += 0.04; gauge.update(); });
    tick.start(16);
    long long start = bench_now_ms();
    long cpu0 = bench_cpu_ticks();
    QTimer::singleShot(bench_seconds() * 1000, [&] {
        bench_report(gauge.frames, bench_now_ms() - start, bench_cpu_ticks() - cpu0);
        app.quit();
    });
    return app.exec();
}
