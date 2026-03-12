package com.quant;

import com.quant.trade.dto.UiNode;
import com.quant.trade.support.AndroidUiNodeLocator;
import com.quant.trade.support.UiHierarchyParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UiHierarchyParserTest {

    private final UiHierarchyParser parser = new UiHierarchyParser();
    private final AndroidUiNodeLocator locator = new AndroidUiNodeLocator();

    @Test
    void shouldParseDumpAndLocateNodeCenter() {
        String xml = """
                <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
                <hierarchy rotation="0">
                  <node index="0" text="持仓" resource-id="com.demo:id/tab_holdings" class="android.widget.TextView"
                        package="com.demo" content-desc="" clickable="true" bounds="[0,0][200,100]"/>
                  <node index="1" text="贵州茅台" resource-id="com.demo:id/name" class="android.widget.TextView"
                        package="com.demo" content-desc="" clickable="false" bounds="[0,100][300,180]"/>
                </hierarchy>
                """;

        List<UiNode> nodes = parser.parse(xml);

        assertThat(nodes).hasSize(2);
        UiNode holdingsTab = locator.findByTextContains(nodes, "持仓");
        assertThat(holdingsTab).isNotNull();
        assertThat(locator.center(holdingsTab)).containsExactly(100, 50);
    }
}
