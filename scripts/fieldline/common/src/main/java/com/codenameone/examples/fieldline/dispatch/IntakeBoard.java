package com.codenameone.examples.fieldline.dispatch;

import com.codename1.appkit.Net;
import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanLane;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.kanban.KanbanAdapter;
import com.codename1.ui.kanban.KanbanBoard;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.examples.fieldline.api.IntakeDto;
import com.codenameone.examples.fieldline.api.JobDto;
import com.codenameone.examples.fieldline.net.Api;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Layouts;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.HashSet;
import java.util.Set;

/// The intake board: the jobs that are not on anyone's route yet, by why not.
///
/// It is a status view beside the Gantt board, never a second way to plan. A
/// card dropped in another column asks the server to move the job between
/// the intake states -- ready to schedule, awaiting parts, blocked -- and the
/// server decides: the role, the transition and the job's state are all
/// checked there. Until it answers the card sits where it was dropped; a
/// refusal, an error or no connection puts it back and says why. Nothing is
/// scheduled from here: the Scheduled column takes no drops, and "Schedule on
/// board" opens the Gantt, where every placement is checked against skills,
/// hours and travel.
public final class IntakeBoard {
    static final String READY = "UNASSIGNED";
    static final String PARTS = "AWAITING_PARTS";
    static final String BLOCKED = "BLOCKED";
    static final String SCHEDULED = "SCHEDULED";

    private final Form form;
    private final Runnable openGantt;
    private final Container root = new Container(new BorderLayout());
    private final DefaultKanbanModel model = new DefaultKanbanModel();
    private final KanbanBoard board = new KanbanBoard(model);
    /// Cards whose move the server has not answered yet; they hold still.
    private final Set<String> pending = new HashSet<String>();

    private IntakeBoard(Form form, Runnable openGantt) {
        this.form = form;
        this.openGantt = openGantt;
        root.setName("intake");
        model.addColumn(new KanbanColumn(READY, Lang.tr("Ready to schedule")));
        model.addColumn(new KanbanColumn(PARTS, Lang.tr("Awaiting parts")));
        model.addColumn(new KanbanColumn(BLOCKED, Lang.tr("Blocked")));
        model.addColumn(new KanbanColumn(SCHEDULED, Lang.tr("Scheduled")));
        board.setName("intakeBoard");
        // Drags only. Cards are jobs, made and named elsewhere, and an undo
        // would move a card without asking the server.
        board.setInlineAdd(false);
        board.setInlineEditing(false);
        board.setColumnReorderEnabled(false);
        board.setUndoHistory(null);
        // A phone shows one column at a time and comes to rest on one.
        board.setColumnWidth(48);
        board.setColumnSnap(!Layouts.wide());
        board.addKanbanListener(new Listener());
        root.add(BorderLayout.CENTER, board);
        root.add(BorderLayout.SOUTH, Ui.primary("Schedule on board", "scheduleOnBoard",
                e -> openGantt.run()));
    }

    /// The board as a screen of its own, on a phone.
    public static void show(final Form previous) {
        final Form form = Ui.form("Intake", "Intake");
        Ui.back(form, () -> previous.showBack());
        IntakeBoard intake = new IntakeBoard(form, () -> previous.showBack());
        form.add(BorderLayout.CENTER, intake.root);
        form.show();
        intake.load();
    }

    /// The board as a page of the console; `openGantt` goes to the board.
    static Container content(Form form, Runnable openGantt) {
        IntakeBoard intake = new IntakeBoard(form, openGantt);
        intake.load();
        return intake.root;
    }

    private void load() {
        Api.dispatch().board(0, 5, Net.to(b -> fill(b.jobs()), (status, message) -> {
            Ui.fail(status, message);
            if (model.getCardCount(model.getColumns().get(0)) == 0) {
                root.removeComponent(board);
                root.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_CLOUD_OFF, message,
                        "Intake needs the server; nothing was changed."));
                form.revalidate();
            }
        }));
    }

    private void fill(java.util.List<JobDto> jobs) {
        for (KanbanColumn c : model.getColumns()) {
            for (KanbanCard card : new java.util.ArrayList<KanbanCard>(model.getCards(c, null))) {
                model.removeCard(card);
            }
        }
        for (JobDto j : jobs) {
            String status = j.status();
            if (!READY.equals(status) && !PARTS.equals(status) && !BLOCKED.equals(status)
                    && !SCHEDULED.equals(status)) {
                continue;
            }
            KanbanCard card = new KanbanCard(j.id(), j.title());
            card.setDescription(j.siteName());
            card.addTag(Lang.tr(kindName(j.kind())));
            if (SCHEDULED.equals(status) && j.techName() != null && j.techName().length() > 0) {
                card.setBadge(j.techName());
            }
            card.setUserData(j);
            model.addCard(status, card);
        }
        board.modelChanged();
        form.revalidate();
    }

    private static final String[] KINDS = {"REPAIR", "INSPECTION", "MAINTENANCE", "INSTALL"};
    private static final String[] KIND_NAMES = {"Repair", "Inspection", "Maintenance", "Install"};

    private static String kindName(String kind) {
        for (int iter = 0; iter < KINDS.length; iter++) {
            if (KINDS[iter].equals(kind)) {
                return KIND_NAMES[iter];
            }
        }
        return kind == null ? "" : kind;
    }

    private KanbanColumn column(String id) {
        for (KanbanColumn c : model.getColumns()) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /// Puts a card back where the server says the job is, without asking
    /// anyone: the listeners are for the user's drags.
    private void snapBack(KanbanCard card, String columnId, int index) {
        KanbanColumn c = column(columnId);
        if (c != null) {
            model.moveCard(card, c, null, Math.min(index, model.getCardCount(c)));
        }
        board.modelChanged();
        board.repaint();
    }

    private final class Listener extends KanbanAdapter {
        private String fromColumn;
        private int fromIndex;

        @Override
        public boolean cardMoving(KanbanBoard b, KanbanCard card, KanbanColumn to, KanbanLane lane,
                int index) {
            if (pending.contains(card.getId())) {
                return false;
            }
            if (SCHEDULED.equals(to.getId()) && !SCHEDULED.equals(card.getColumnId())) {
                Ui.say("Jobs are scheduled on the board, not here");
                return false;
            }
            fromColumn = card.getColumnId();
            fromIndex = model.getCards(column(fromColumn), null).indexOf(card);
            return true;
        }

        @Override
        public void cardMoved(KanbanBoard b, final KanbanCard card) {
            final String from = fromColumn;
            final int at = fromIndex;
            final String to = card.getColumnId();
            if (to == null || to.equals(from)) {
                return;
            }
            pending.add(card.getId());
            Api.dispatch().intake(card.getId(), new IntakeDto(to), Net.to(job -> {
                pending.remove(card.getId());
                card.setUserData(job);
                card.setBadge(null);
                if (job != null && !to.equals(job.status())) {
                    snapBack(card, job.status(), at);
                }
            }, (status, message) -> {
                pending.remove(card.getId());
                snapBack(card, from, at);
                Ui.fail(status, message);
            }));
        }

        @Override
        public void cardOpened(KanbanBoard b, KanbanCard card) {
            openGantt.run();
        }

        @Override
        public boolean cardAdding(KanbanBoard b, KanbanCard card, KanbanColumn column,
                KanbanLane lane) {
            return false;
        }

        @Override
        public boolean cardRenaming(KanbanBoard b, KanbanCard card, String title) {
            return false;
        }

        @Override
        public boolean cardRemoving(KanbanBoard b, KanbanCard card) {
            return false;
        }

        @Override
        public boolean columnMoving(KanbanBoard b, KanbanColumn column, int index) {
            return false;
        }
    }
}
