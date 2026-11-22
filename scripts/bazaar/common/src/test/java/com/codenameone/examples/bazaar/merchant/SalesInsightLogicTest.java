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

package com.codenameone.examples.bazaar.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.ai.ChatMessage;
import com.codename1.ai.ChatRequest;
import com.codename1.ai.ChatResponse;
import com.codename1.ai.EmbeddingRequest;
import com.codename1.ai.EmbeddingResponse;
import com.codename1.ai.LlmClient;
import com.codename1.ai.MessagePart;
import com.codename1.ai.Role;
import com.codename1.ai.StreamingListener;
import com.codename1.ai.ToolCall;
import com.codename1.ai.ToolResultPart;
import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.util.AsyncResource;
import com.codenameone.examples.bazaar.api.AnalyticsDto;
import com.codenameone.examples.bazaar.api.BreakdownDto;
import com.codenameone.examples.bazaar.api.FunnelStepDto;
import com.codenameone.examples.bazaar.api.InsightsDto;
import com.codenameone.examples.bazaar.api.KpiDto;
import com.codenameone.examples.bazaar.api.PipelineCardDto;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.api.TopItemDto;
import com.codenameone.examples.bazaar.ops.SalesPipeline;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The sales pipeline board, the insight charts and the merchant assistant's
/// tools and conversation, without a component or a network.
class SalesInsightLogicTest {
    private static final long NOW = 1700000000000L;

    private static PipelineCardDto cart(String id, int intent, String... reasons) {
        return new PipelineCardDto(id, SalesPipeline.CART, 0, "ana@example.com", 12500L, "USD",
                SalesPipeline.CHECKOUT, NOW - 7200000L, NOW - 600000L, intent,
                Arrays.asList(reasons));
    }

    private static PipelineCardDto order(String id) {
        return new PipelineCardDto(id, SalesPipeline.ORDER, 1042, "Bo", 4000L, "USD",
                SalesPipeline.PAID, NOW - 3 * 3600000L, NOW, -1, Collections.<String>emptyList());
    }

    private static List<PipelineColumnDto> board() {
        List<PipelineColumnDto> out = new ArrayList<PipelineColumnDto>();
        // Out of order on purpose, with one column the app does not know.
        out.add(new PipelineColumnDto(SalesPipeline.PAID, "Paid", 1, 0, false,
                Arrays.asList(order("o1"))));
        out.add(new PipelineColumnDto("LATER", "Later", 0, 0, false,
                Collections.<PipelineCardDto>emptyList()));
        out.add(new PipelineColumnDto(SalesPipeline.CHECKOUT, "Checkout started", 2, 0, false,
                Arrays.asList(cart("c1", 82, "views:6", "returning:2", "value:12500"),
                        cart("c2", 20, "views:1"))));
        return out;
    }

    @Test
    void theBoardFollowsThePipelineAndExplainsIntent() {
        List<PipelineColumnDto> ordered = Pipelines.ordered(board());
        assertEquals(SalesPipeline.CHECKOUT, ordered.get(0).id());
        assertEquals(SalesPipeline.PAID, ordered.get(1).id());
        assertEquals("LATER", ordered.get(2).id());
        assertEquals("Checkout started 2", Pipelines.columnTitle(ordered.get(0), Pipelines.PLAIN));

        DefaultKanbanModel model = Pipelines.model(board(), NOW, Pipelines.PLAIN);
        KanbanCard hot = model.getCard("c1");
        assertEquals("ana@example.com", hot.getTitle());
        assertEquals("High intent 82", hot.getBadge());
        assertEquals("125.00 USD  10m", hot.getDescription());
        assertEquals(Arrays.asList("Viewed products 6 times",
                "Returning customer, 2 orders before"), hot.getTags());
        KanbanCard paid = model.getCard("o1");
        assertEquals("#1042  Bo", paid.getTitle());
        assertEquals("3h", paid.getBadge());

        assertEquals(Arrays.asList("Viewed products 6 times", "Returning customer, 2 orders "
                + "before", "Cart worth 125.00 USD"), Pipelines.explain(cart("x", 82, "views:6",
                "returning:2", "value:12500", "bogus:1"), Pipelines.PLAIN));
        assertEquals("Viewed a product once", Pipelines.explain(cart("y", 3, "views:1"),
                Pipelines.PLAIN).get(0));
        assertEquals("Low intent {0}", Pipelines.bandKey(10));
        assertEquals("Medium intent {0}", Pipelines.bandKey(45));
        assertEquals("Send reminder", Pipelines.moveLabel(SalesPipeline.CHECKOUT));
        assertEquals("Pack", Pipelines.moveLabel(SalesPipeline.PAID));
        assertNull(Pipelines.moveLabel(SalesPipeline.BROWSING));
        assertNull(Pipelines.moveLabel(SalesPipeline.DELIVERED));
    }

    @Test
    void theInsightChartsDropEmptyChannelsAndShareTheFunnel() {
        List<BreakdownDto> channels = Arrays.asList(new BreakdownDto("web", "Online store", 3,
                9000L), new BreakdownDto("pos", "Point of sale", 0, 0L));
        assertEquals(1, Insights.withSales(channels).size());
        int[] shares = Insights.shares(funnel());
        assertEquals(10000, shares[0]);
        assertEquals(2500, shares[1]);
        assertEquals(500, shares[3]);
        assertEquals(0, Insights.shares(Arrays.asList(new FunnelStepDto("views", "Views", 0)))[0]);
    }

    private static List<FunnelStepDto> funnel() {
        return Arrays.asList(new FunnelStepDto("views", "Product views", 200),
                new FunnelStepDto("carts", "Carts", 50),
                new FunnelStepDto("checkouts", "Checkouts", 30),
                new FunnelStepDto("orders", "Orders", 10));
    }

    private static AssistantTools tools() {
        KpiDto now = new KpiDto(0, 0, 0, 125000L, 0, 0, 10, 12500L, 8, 0, 0, 0);
        KpiDto was = new KpiDto(0, 0, 0, 100000L, 0, 0, 8, 12500L, 7, 0, 0, 0);
        AnalyticsDto data = new AnalyticsDto(0, 0, 0, now, was, null, null, null, null,
                Arrays.asList(new TopItemDto("p1", "Linen Shirt", null, 7, 70000L)), null, null,
                null, null, null, "USD");
        InsightsDto in = new InsightsDto(0, 0, Arrays.asList(
                new BreakdownDto("web", "Online store", 6, 80000L),
                new BreakdownDto("app", "App", 4, 45000L)), funnel(), "USD");
        return new AssistantTools(data, in, board(), "USD");
    }

    @Test
    void theToolsAnswerWithTheNumbersAndASummary() {
        AssistantTools t = tools();
        assertEquals(5, t.tools().size());
        String sales = t.run(AssistantTools.SALES);
        assertTrue(sales.contains("Net sales were 1250.00 USD from 10 orders"), sales);
        assertTrue(sales.contains("Up 25%"), sales);
        String ch = t.run(AssistantTools.CHANNELS);
        assertTrue(ch.contains("Online store sold the most: 800.00 USD from 6 orders, 64% of "
                + "sales."), ch);
        String f = t.run(AssistantTools.FUNNEL);
        assertTrue(f.contains("200 product views, then 50 carts"), f);
        assertTrue(f.contains("5.00% of the first step reached the last"), f);
        assertTrue(f.contains("the biggest drop is from product views to carts"), f);
        String p = t.run(AssistantTools.PIPELINE);
        assertTrue(p.contains("Open now: 1 paid, 2 checkout started"), p);
        assertTrue(p.contains("worth 125.00 USD, intent 82: viewed products 6 times"), p);
        assertTrue(t.run(AssistantTools.TOP).contains("Linen Shirt (7 sold, 700.00 USD)"));
        assertTrue(t.run("nope").contains("unknown tool"));
        assertEquals("Down 50%", AssistantTools.change(50, 100));
        assertEquals("Level", AssistantTools.change(5, 5));
        assertTrue(new AssistantTools(null, null, null, "USD").run(AssistantTools.SALES)
                .contains("not loaded"));
    }

    /// A model that asks for one tool and then answers with what came back.
    private static final class FakeModel extends LlmClient {
        final List<ChatRequest> seen = new ArrayList<ChatRequest>();

        FakeModel() {
            super("http://localhost");
        }

        @Override
        public AsyncResource<ChatResponse> chat(ChatRequest req) {
            seen.add(req);
            AsyncResource<ChatResponse> out = new AsyncResource<ChatResponse>();
            List<ChatMessage> m = req.getMessages();
            ChatMessage last = m.get(m.size() - 1);
            if (last.getRole() == Role.TOOL) {
                out.complete(new ChatResponse(ChatMessage.assistant("From the tool: "
                        + ((ToolResultPart) last.getParts().get(0)).getResultJson()), null,
                        "stop", null, "fake"));
            } else {
                List<ToolCall> calls = Arrays.asList(new ToolCall("c1",
                        AssistantTools.CHANNELS, "{}"));
                out.complete(new ChatResponse(new ChatMessage(Role.ASSISTANT,
                        new ArrayList<MessagePart>(), calls, null, null), calls, "tool_calls",
                        null, "fake"));
            }
            return out;
        }

        @Override
        public AsyncResource<ChatResponse> chatStream(ChatRequest req, StreamingListener l) {
            return chat(req);
        }

        @Override
        public AsyncResource<EmbeddingResponse> embed(EmbeddingRequest req) {
            return new AsyncResource<EmbeddingResponse>();
        }

        @Override
        public String getProvider() {
            return "fake";
        }
    }

    @Test
    void theAssistantRunsTheToolTheModelAsksForAndAnswers() {
        FakeModel model = new FakeModel();
        final String[] answer = new String[1];
        new AssistantRun(model, tools()).ask(null, "Which channel?", new AssistantRun.Done() {
            @Override
            public void answered(String text) {
                answer[0] = text;
            }

            @Override
            public void failed(Throwable error) {
                answer[0] = "failed " + error;
            }
        });
        assertTrue(answer[0].startsWith("From the tool: "), answer[0]);
        assertTrue(answer[0].contains("Online store sold the most"), answer[0]);
        assertEquals(2, model.seen.size());
        ChatRequest first = model.seen.get(0);
        assertEquals("bazaar:assistant", first.getMessages().get(0).getText());
        assertEquals(5, first.getTools().size());
        assertFalse(model.seen.get(1).getMessages().get(2).getToolCalls().isEmpty());
        // Empty fields are left out; the JSON writer's spacing is not the point.
        String json = DescriptionDraft.fields(" Linen Shirt ", "", null, "  ", "Size: S, M");
        assertTrue(json.contains("\"Linen Shirt\""), json);
        assertTrue(json.contains("\"Size: S, M\""), json);
        assertFalse(json.contains("vendor") || json.contains("type") || json.contains("tags"),
                json);
    }
}
