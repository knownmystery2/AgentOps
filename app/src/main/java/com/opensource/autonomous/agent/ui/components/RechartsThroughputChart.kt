package com.opensource.autonomous.agent.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.google.gson.Gson
import com.opensource.autonomous.agent.data.LedgerEntity
import java.text.SimpleDateFormat
import java.util.*

data class DailyLedgerMetric(
    val date: String,
    val sales: Double,
    val purchases: Double,
    val netProfit: Double,
    val count: Int
)

/**
 * RechartsThroughputChart
 *
 * Uses the Recharts library (via embedded interactive HTML5/WebView) to visualize the
 * transaction throughput of the local Room ledger, displaying daily activity trends
 * for sales and purchases.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsThroughputChart(
    ledger: List<LedgerEntity>,
    modifier: Modifier = Modifier
) {
    val dailyMetrics = remember(ledger) {
        aggregateLedgerByDay(ledger)
    }

    val jsonMetrics = remember(dailyMetrics) {
        Gson().toJson(dailyMetrics)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_throughput_container"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transaction Throughput",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Visualized with Recharts • Daily Sales & Purchases Trends",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Recharts v2.12",
                        color = BrandEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recharts WebView Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            setBackgroundColor(0xFF0F172A.toInt())
                            webViewClient = WebViewClient()
                            loadDataWithBaseURL(
                                null,
                                buildRechartsHtml(jsonMetrics),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            null,
                            buildRechartsHtml(jsonMetrics),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxSize().testTag("recharts_webview")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Stats Pill Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ThroughputPill(
                    label = "Total Sales",
                    value = "$${String.format("%.2f", dailyMetrics.sumOf { it.sales })}",
                    color = Color(0xFF10B981)
                )
                ThroughputPill(
                    label = "Total Purchases (COGS)",
                    value = "$${String.format("%.2f", dailyMetrics.sumOf { it.purchases })}",
                    color = Color(0xFF3B82F6)
                )
                ThroughputPill(
                    label = "Total Volume",
                    value = "${dailyMetrics.sumOf { it.count }} txns",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThroughputPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * Aggregates raw ledger transactions into daily buckets for Recharts time-series chart
 */
private fun aggregateLedgerByDay(ledger: List<LedgerEntity>): List<DailyLedgerMetric> {
    if (ledger.isEmpty()) {
        // Provide sample baseline days if ledger is fresh
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        return listOf(
            DailyLedgerMetric(sdf.format(Date(now - dayMs * 3)), 0.0, 0.0, 0.0, 0),
            DailyLedgerMetric(sdf.format(Date(now - dayMs * 2)), 0.0, 0.0, 0.0, 0),
            DailyLedgerMetric(sdf.format(Date(now - dayMs)), 0.0, 0.0, 0.0, 0),
            DailyLedgerMetric(sdf.format(Date(now)), 0.0, 0.0, 0.0, 0)
        )
    }

    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    val grouped = ledger.groupBy { sdf.format(Date(it.timestamp)) }

    return grouped.map { (dateStr, entries) ->
        val salesCents = entries.filter { it.category == "SALE" }.sumOf { it.amountCents }
        val cogsCents = entries.filter { it.category == "COGS" }.sumOf { it.amountCents }
        val sales = salesCents / 100.0
        val purchases = cogsCents / 100.0
        val net = sales - purchases
        DailyLedgerMetric(
            date = dateStr,
            sales = sales,
            purchases = purchases,
            netProfit = net,
            count = entries.size
        )
    }.sortedBy { it.date }
}

/**
 * Generates lightweight, robust HTML using Recharts to visualize time series data
 */
private fun buildRechartsHtml(dataJson: String): String {
    return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <script src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
  <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
  <script src="https://unpkg.com/prop-types@15.8.1/prop-types.min.js"></script>
  <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.min.js"></script>
  <style>
    body {
      margin: 0;
      padding: 0;
      background-color: #0F172A;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      overflow: hidden;
    }
    #root {
      width: 100vw;
      height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  </style>
</head>
<body>
  <div id="root"></div>
  <script>
    const data = $dataJson;
    const { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend, CartesianGrid } = window.Recharts || {};

    if (!window.Recharts) {
      document.getElementById('root').innerHTML = '<div style="color:#10B981;font-size:12px;text-align:center;">Loading Recharts engine...</div>';
    } else {
      const App = () => {
        return React.createElement(
          ResponsiveContainer,
          { width: '100%', height: '95%' },
          React.createElement(
            AreaChart,
            { data: data, margin: { top: 10, right: 20, left: -10, bottom: 0 } },
            React.createElement('defs', null,
              React.createElement('linearGradient', { id: 'salesGrad', x1: '0', y1: '0', x2: '0', y2: '1' },
                React.createElement('stop', { offset: '5%', stopColor: '#10B981', stopOpacity: 0.8 }),
                React.createElement('stop', { offset: '95%', stopColor: '#10B981', stopOpacity: 0 })
              ),
              React.createElement('linearGradient', { id: 'purchasesGrad', x1: '0', y1: '0', x2: '0', y2: '1' },
                React.createElement('stop', { offset: '5%', stopColor: '#3B82F6', stopOpacity: 0.8 }),
                React.createElement('stop', { offset: '95%', stopColor: '#3B82F6', stopOpacity: 0 })
              )
            ),
            React.createElement(CartesianGrid, { strokeDasharray: '3 3', stroke: '#1E293B' }),
            React.createElement(XAxis, { dataKey: 'date', stroke: '#94A3B8', fontSize: 10 }),
            React.createElement(YAxis, { stroke: '#94A3B8', fontSize: 10 }),
            React.createElement(Tooltip, {
              contentStyle: { backgroundColor: '#1E293B', borderColor: '#334155', borderRadius: '8px', color: '#F8FAFC', fontSize: '11px' }
            }),
            React.createElement(Legend, { wrapperStyle: { fontSize: '11px', color: '#94A3B8' } }),
            React.createElement(Area, {
              type: 'monotone',
              dataKey: 'sales',
              name: 'Sales ($)',
              stroke: '#10B981',
              fillOpacity: 1,
              fill: 'url(#salesGrad)'
            }),
            React.createElement(Area, {
              type: 'monotone',
              dataKey: 'purchases',
              name: 'Purchases / COGS ($)',
              stroke: '#3B82F6',
              fillOpacity: 1,
              fill: 'url(#purchasesGrad)'
            })
          )
        );
      };

      ReactDOM.render(React.createElement(App), document.getElementById('root'));
    }
  </script>
</body>
</html>
    """.trimIndent()
}
