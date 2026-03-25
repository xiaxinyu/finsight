function cssVar(name){
    return getComputedStyle(document.documentElement).getPropertyValue('--'+name).trim() || undefined;
}
var consumptionChart = null;
var behaviorChart = null;
var latestSummaryRequest = null;

document.addEventListener('DOMContentLoaded', function() {
    initYearSelect();
    loadHomeSummary(new Date().getFullYear());
    window.addEventListener('resize', onResizeCharts);
});

function initYearSelect(){
    var currentYear = new Date().getFullYear();
    var yearSelect = document.getElementById('yearSelect');
    for (var y = currentYear; y >= currentYear - 5; y--) {
        var option = document.createElement('option');
        option.value = String(y);
        option.text = String(y);
        if (y === currentYear) {
            option.selected = true;
        }
        yearSelect.appendChild(option);
    }
    yearSelect.addEventListener('change', function(e){
        loadHomeSummary(Number(e.target.value));
    });
}

function onResizeCharts(){
    if (consumptionChart) {
        consumptionChart.resize();
    }
    if (behaviorChart) {
        behaviorChart.resize();
    }
}

async function loadHomeSummary(year){
    var selectedYear = Number(year) || new Date().getFullYear();
    var url = '/transaction-report/home-summary?year=' + selectedYear;
    if (latestSummaryRequest) {
        latestSummaryRequest.abort();
    }
    latestSummaryRequest = new AbortController();
    setLoadingState();
    try{
        var resp = await fetch(url, { method: 'GET', signal: latestSummaryRequest.signal });
        var data = await resp.json();
        var normalized = (window.app && app.api && app.api.normalizeResult)
            ? app.api.normalizeResult(data)
            : { ok: false, data: null };
        if (!normalized || !normalized.ok) {
            setErrorState('No summary data for selected year');
            setEmptyState(true);
            initConsumptionChart();
            initBehaviorChart();
            hideLoadingState();
            return;
        }
        var payloadRaw = typeof normalized.data === 'string' ? normalized.data : (data.returnMessage || '{}');
        var payload = safeJsonParse(payloadRaw);
        var buckets = payload.buckets_pct || {};
        var hasData = hasSummaryData(payload);
        setEmptyState(!hasData);
        var resolvedYear = payload.year || selectedYear;
        document.getElementById('pageMeta').innerText = 'Year: ' + resolvedYear;
        document.getElementById('kpiYear').innerText = String(resolvedYear);
        document.getElementById('kpiInsight').innerText = (payload.summary_text || '—').replace('📌 ', '') || '—';
        document.getElementById('kpiTopBucket').innerText = findTopBucketText(buckets) || '—';
        var arr = [
            { value: buckets.life || 0, name: 'Life (Essentials)' },
            { value: buckets.fixed || 0, name: 'Fixed Costs' },
            { value: buckets.shopping || 0, name: 'Shopping' },
            { value: buckets.entertainment || 0, name: 'Entertainment' },
            { value: buckets.investment || 0, name: 'Investment' },
            { value: buckets.edu || 0, name: 'Education' },
            { value: buckets.other || 0, name: 'Other' }
        ];
        initConsumptionChart(arr);
        document.getElementById('consumptionTitle').innerText = resolvedYear + ' Consumption Structure';
        document.getElementById('lifePct').innerText = (buckets.life || 0) + '%';
        document.getElementById('fixedPct').innerText = (buckets.fixed || 0) + '%';
        document.getElementById('shoppingPct').innerText = (buckets.shopping || 0) + '%';
        document.getElementById('entertainmentPct').innerText = (buckets.entertainment || 0) + '%';
        document.getElementById('investmentPct').innerText = (buckets.investment || 0) + '%';
        document.getElementById('eduPct').innerText = (buckets.edu || 0) + '%';
        document.getElementById('otherPct').innerText = (buckets.other || 0) + '%';
        document.getElementById('summaryText').innerText = (payload.summary_text || '').replace('📌 ', '');

        var hs = payload.health_score || {};
        var dims = hs.dimensions || {};
        var radarValues = [
            toNumber(dims.spend_control),
            toNumber(dims.savings_rate),
            toNumber(dims.invest_awareness),
            toNumber(dims.debt_risk),
            toNumber(dims.rationality),
            toNumber(dims.growth_trend)
        ];
        initBehaviorChart(radarValues);
        applyScore(hs.total);
        document.getElementById('spendControlText').innerText = Math.round(dims.spend_control || 0) + ' / 100';
        document.getElementById('savingsRateText').innerText = Math.round(dims.savings_rate || 0) + ' / 100';
        hideLoadingState();
    }catch(e){
        if (e && e.name === 'AbortError') {
            return;
        }
        setErrorState('Failed to load summary');
        setEmptyState(true);
        initConsumptionChart();
        initBehaviorChart();
        hideLoadingState();
    }
}

function safeJsonParse(raw){
    if (!raw || typeof raw !== 'string') return {};
    try {
        return JSON.parse(raw);
    } catch (e) {
        return {};
    }
}
function setLoadingState(){
    document.getElementById('kpiInsight').innerText = 'Loading...';
    document.getElementById('summaryText').innerText = 'Loading...';
    toggleLoading('consumptionLoading', true);
    toggleLoading('behaviorLoading', true);
}
function setErrorState(message){
    document.getElementById('kpiInsight').innerText = message;
    document.getElementById('summaryText').innerText = message;
}
function hideLoadingState(){
    toggleLoading('consumptionLoading', false);
    toggleLoading('behaviorLoading', false);
}
function toggleLoading(id, loading){
    var el = document.getElementById(id);
    if (!el) return;
    if (loading) {
        el.classList.add('show');
    } else {
        el.classList.remove('show');
    }
}
function setEmptyState(empty){
    toggleEmpty('consumptionEmpty', empty);
    toggleEmpty('behaviorEmpty', empty);
}
function toggleEmpty(id, empty){
    var el = document.getElementById(id);
    if (!el) return;
    if (empty) {
        el.classList.add('show');
    } else {
        el.classList.remove('show');
    }
}
function hasSummaryData(payload){
    if (!payload || typeof payload !== 'object') return false;
    var expense = Number(payload.expense_total || 0);
    var income = Number(payload.income_total || 0);
    var score = Number((payload.health_score || {}).total || 0);
    return expense > 0 || income > 0 || score > 0;
}
function toNumber(v){ var n = Number(v); return isNaN(n)?0:n; }

function applyScore(total){
    var scoreEl = document.getElementById('scoreBadge');
    var kpiScoreEl = document.getElementById('kpiScore');
    var n = Number(total);
    var text = (isNaN(n) ? '--' : String(Math.round(n)));
    scoreEl.classList.remove('score-good', 'score-warn', 'score-bad');
    if (!isNaN(n)) {
        if (n < 60) scoreEl.classList.add('score-bad');
        else if (n < 80) scoreEl.classList.add('score-warn');
        else scoreEl.classList.add('score-good');
    }
    scoreEl.innerText = text;
    kpiScoreEl.innerText = text;
}

function findTopBucketText(buckets){
    var defs = [
        { key: 'life', label: 'Life Essentials' },
        { key: 'fixed', label: 'Fixed Costs' },
        { key: 'shopping', label: 'Shopping' },
        { key: 'entertainment', label: 'Entertainment' },
        { key: 'investment', label: 'Investment' },
        { key: 'edu', label: 'Education' },
        { key: 'other', label: 'Other' }
    ];
    var maxKey = null;
    var maxVal = -1;
    for (var i = 0; i < defs.length; i++) {
        var v = Number((buckets || {})[defs[i].key] || 0);
        if (!isNaN(v) && v > maxVal) {
            maxVal = v;
            maxKey = defs[i].key;
        }
    }
    if (maxKey === null) return '';
    var label = '';
    for (var j = 0; j < defs.length; j++) {
        if (defs[j].key === maxKey) { label = defs[j].label; break; }
    }
    return label + ' · ' + Math.round(maxVal) + '%';
}

function initConsumptionChart(dataArr) {
    var chartDom = document.getElementById('consumptionChart');
    if (!consumptionChart) {
        consumptionChart = echarts.init(chartDom);
    }
    var option = {
        tooltip: {
            trigger: 'item',
            formatter: function(p){
                var v = (p && p.value != null) ? p.value : '--';
                return p.name + ': ' + v + '%';
            }
        },
        legend: {
            bottom: '0%',
            left: 'center',
            icon: 'roundRect',
            itemWidth: 12,
            itemHeight: 12,
            textStyle: { color: '#6b7280', fontSize: 12 }
        },
        color: [
            cssVar('chart-life') || '#1890FF',
            cssVar('chart-fixed') || '#52C41A',
            cssVar('chart-shopping') || '#FAAD14',
            cssVar('chart-entertainment') || '#F5222D',
            cssVar('chart-investment') || '#722ED1',
            cssVar('chart-education') || '#13C2C2',
            cssVar('chart-other') || '#BFBFBF'
        ],
        series: [
            {
                name: 'Consumption',
                type: 'pie',
                radius: ['50%', '70%'],
                avoidLabelOverlap: false,
                itemStyle: {
                    borderRadius: 8,
                    borderColor: '#fff',
                    borderWidth: 2
                },
                label: {
                    show: false,
                    position: 'center'
                },
                emphasis: {
                    scale: true,
                    scaleSize: 10
                },
                data: dataArr || [
                    { value: 58, name: 'Life (Essentials)' },
                    { value: 15, name: 'Fixed Costs' },
                    { value: 12, name: 'Shopping' },
                    { value: 8, name: 'Entertainment' },
                    { value: 7, name: 'Investment' }
                ]
            }
        ]
    };
    consumptionChart.setOption(option, true);
}

function initBehaviorChart(values) {
    var chartDom = document.getElementById('behaviorChart');
    if (!behaviorChart) {
        behaviorChart = echarts.init(chartDom);
    }
    var option = {
        radar: {
            indicator: [
                { name: 'Spend Control', max: 100 },
                { name: 'Savings Rate', max: 100 },
                { name: 'Invest Awareness', max: 100 },
                { name: 'Debt Risk', max: 100 },
                { name: 'Rationality', max: 100 },
                { name: 'Growth Trend', max: 100 }
            ],
            shape: 'circle',
            radius: '65%',
            splitNumber: 5,
            axisName: { color: '#595959', fontSize: 12 },
            axisLine: { show: false },
            splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)' } },
            splitArea: {
                show: true,
                areaStyle: { color: ['rgba(24,144,255,0.02)', 'rgba(24,144,255,0.04)', 'rgba(24,144,255,0.06)', 'rgba(24,144,255,0.08)', 'rgba(24,144,255,0.10)'] }
            }
        },
        series: [
            {
                name: 'Health Score',
                type: 'radar',
                data: [
                    {
                        value: values || [90, 85, 70, 95, 80, 88],
                        name: 'Current Status',
                        areaStyle: {
                            color: 'rgba(82, 196, 26, 0.30)'
                        },
                        lineStyle: {
                            color: '#52C41A',
                            width: 2
                        },
                        itemStyle: {
                            color: '#52C41A'
                        }
                    }
                ]
            }
        ]
    };
    behaviorChart.setOption(option, true);
}
