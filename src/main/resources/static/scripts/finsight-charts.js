/**
 * ECharts defaults aligned with FinSight chart UI spec (Chart.js-equivalent rules).
 */
var finsightCharts = (function () {
    'use strict';

    var AXIS_FONT = 11;
    var LEGEND_FONT = 12;
    var TOOLTIP_FONT = 12;
    var TITLE_FONT = 14;

    var BASELINE_NAMES = /(\(B\)|baseline|benchmark|prior|previous|last year|同比|环比)/i;
    var ACTUAL_NAMES = /(\(A\)|actual|current|本年|今年)/i;

    function isObject(v) {
        return v && typeof v === 'object' && !Array.isArray(v);
    }

    function deepMerge(target, source) {
        if (!isObject(source)) {
            return target;
        }
        var out = isObject(target) ? target : {};
        Object.keys(source).forEach(function (key) {
            var sv = source[key];
            var tv = out[key];
            if (Array.isArray(sv)) {
                out[key] = sv.slice();
            } else if (isObject(sv)) {
                out[key] = deepMerge(isObject(tv) ? tv : {}, sv);
            } else {
                out[key] = sv;
            }
        });
        return out;
    }

    function parseDate(raw) {
        if (!raw) {
            return null;
        }
        var s = String(raw).trim();
        var m = /^(\d{4})-(\d{1,2})-(\d{1,2})/.exec(s);
        if (m) {
            return new Date(+m[1], +m[2] - 1, +m[3]);
        }
        m = /^(\d{1,2})\/(\d{1,2})\/(\d{4})/.exec(s);
        if (m) {
            return new Date(+m[3], +m[1] - 1, +m[2]);
        }
        return null;
    }

    function daySpan(categories) {
        if (!categories || !categories.length) {
            return 0;
        }
        var min = null;
        var max = null;
        for (var i = 0; i < categories.length; i++) {
            var d = parseDate(categories[i]);
            if (!d || isNaN(d.getTime())) {
                continue;
            }
            if (!min || d < min) {
                min = d;
            }
            if (!max || d > max) {
                max = d;
            }
        }
        if (!min || !max) {
            return categories.length;
        }
        return Math.round((max - min) / 86400000) + 1;
    }

    function formatMmDd(value) {
        var d = parseDate(value);
        if (d && !isNaN(d.getTime())) {
            var mm = String(d.getMonth() + 1);
            var dd = String(d.getDate());
            if (mm.length < 2) {
                mm = '0' + mm;
            }
            if (dd.length < 2) {
                dd = '0' + dd;
            }
            return mm + '/' + dd;
        }
        var s = String(value || '');
        if (s.length >= 5 && s.charAt(2) === '/') {
            return s.substring(0, 5);
        }
        return s;
    }

    function formatFullDate(value) {
        var d = parseDate(value);
        if (d && !isNaN(d.getTime())) {
            var y = d.getFullYear();
            var mm = String(d.getMonth() + 1);
            var dd = String(d.getDate());
            if (mm.length < 2) {
                mm = '0' + mm;
            }
            if (dd.length < 2) {
                dd = '0' + dd;
            }
            return y + '-' + mm + '-' + dd;
        }
        return String(value || '');
    }

    function axisLabelInterval(count) {
        if (count <= 0) {
            return 0;
        }
        var target = Math.min(12, Math.max(6, count <= 12 ? count : 10));
        if (count <= target) {
            return 0;
        }
        return Math.ceil(count / target) - 1;
    }

    function categoriesFromOption(option) {
        if (!option) {
            return [];
        }
        var x = option.xAxis;
        if (Array.isArray(x) && x[0] && x[0].data) {
            return x[0].data;
        }
        if (x && x.data) {
            return x.data;
        }
        return [];
    }

    function longestSeries(option) {
        var max = 0;
        (option.series || []).forEach(function (s) {
            var len = s && s.data ? s.data.length : 0;
            if (len > max) {
                max = len;
            }
        });
        return max;
    }

    function styleSeries(series, hideSymbols) {
        if (!Array.isArray(series)) {
            return series;
        }
        return series.map(function (s) {
            var next = deepMerge({}, s || {});
            var name = String(next.name || '');
            if (next.type === 'line') {
                next.emphasis = deepMerge({ focus: 'series', scale: true }, next.emphasis || {});
                if (hideSymbols) {
                    next.showSymbol = false;
                    next.symbol = 'none';
                } else {
                    next.showSymbol = next.showSymbol !== false;
                    next.symbolSize = next.symbolSize || 6;
                }
                if (BASELINE_NAMES.test(name) && !ACTUAL_NAMES.test(name)) {
                    next.lineStyle = deepMerge({ type: 'dashed', width: 2 }, next.lineStyle || {});
                } else if (ACTUAL_NAMES.test(name) || !BASELINE_NAMES.test(name)) {
                    next.lineStyle = deepMerge({ type: 'solid', width: 2 }, next.lineStyle || {});
                }
            }
            return next;
        });
    }

    function mergeAxisDefaults(option, key, axisDefaults) {
        var axis = option[key];
        if (Array.isArray(axis)) {
            option[key] = axis.map(function (ax) {
                return deepMerge(deepMerge({}, axisDefaults), ax || {});
            });
            return;
        }
        if (axis) {
            option[key] = deepMerge(deepMerge({}, axisDefaults), axis);
        } else {
            option[key] = deepMerge({}, axisDefaults);
        }
    }

    function buildDefaults(option, meta) {
        meta = meta || {};
        var categories = meta.categories || categoriesFromOption(option);
        var span = daySpan(categories);
        var rotate = span > 28 ? 35 : 0;
        var interval = axisLabelInterval(categories.length);
        var hideSymbols = longestSeries(option) > 60;

        var xAxisDefaults = {
            axisLabel: {
                fontSize: AXIS_FONT,
                color: '#64748b',
                rotate: rotate,
                interval: interval,
                formatter: function (v) {
                    return formatMmDd(v);
                }
            },
            axisLine: { lineStyle: { color: '#cbd5e1' } },
            axisTick: { alignWithLabel: true }
        };

        var yAxisDefaults = {
            axisLabel: { fontSize: AXIS_FONT, color: '#64748b' },
            splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
            scale: true
        };

        var defaults = {
            title: {
                textStyle: { fontSize: TITLE_FONT, fontWeight: 700, color: '#0f172a' },
                left: meta.titleAlign || 'left',
                top: 8,
                padding: [0, 0, 12, 0]
            },
            legend: {
                textStyle: { fontSize: LEGEND_FONT, color: '#475569' },
                icon: 'roundRect',
                itemWidth: 14,
                itemHeight: 8,
                top: 4
            },
            tooltip: {
                textStyle: { fontSize: TOOLTIP_FONT },
                confine: true,
                axisPointer: {
                    type: 'cross',
                    crossStyle: { color: '#94a3b8', width: 1, type: 'dashed' },
                    label: { backgroundColor: '#334155', fontSize: 11 }
                }
            },
            grid: { left: 56, right: 24, top: 52, bottom: rotate > 0 ? 78 : 64, containLabel: true },
            dataZoom: categories.length > 90 ? [
                { type: 'inside', xAxisIndex: 0 },
                { type: 'slider', xAxisIndex: 0, height: 18, bottom: 8 }
            ] : undefined,
            series: styleSeries(option.series, hideSymbols)
        };

        if (option.tooltip && option.tooltip.trigger === 'item') {
            defaults.tooltip.axisPointer = { type: 'shadow' };
        }

        var merged = deepMerge(defaults, option || {});
        mergeAxisDefaults(merged, 'xAxis', xAxisDefaults);
        mergeAxisDefaults(merged, 'yAxis', yAxisDefaults);
        merged.series = styleSeries(merged.series, hideSymbols);

        var existingFormatter = option.tooltip && option.tooltip.formatter;
        if (typeof existingFormatter === 'function') {
            merged.tooltip.formatter = function (params) {
                if (Array.isArray(params) && params[0]) {
                    params[0].axisValueLabel = formatFullDate(params[0].axisValue || params[0].name);
                }
                return existingFormatter(params);
            };
        } else if (!existingFormatter && merged.tooltip.trigger === 'axis') {
            merged.tooltip.formatter = function (params) {
                if (!params || !params.length) {
                    return '';
                }
                var title = formatFullDate(params[0].axisValue || params[0].name);
                var lines = [title];
                for (var i = 0; i < params.length; i++) {
                    var p = params[i];
                    var val = (p.data && p.data.value != null) ? p.data.value : p.value;
                    lines.push((p.marker || '') + p.seriesName + ': ' + val);
                }
                return lines.join('<br/>');
            };
        }

        return merged;
    }

    function applyDefaults(option, meta) {
        return buildDefaults(option || {}, meta || {});
    }

    function patchEchartsInit() {
        if (!window.echarts || window.echarts.__finsightPatched) {
            return;
        }
        var original = window.echarts.init;
        window.echarts.init = function (dom, theme, opts) {
            var chart = original.call(window.echarts, dom, theme, opts);
            var setOption = chart.setOption.bind(chart);
            chart.setOption = function (option, notMerge, lazyUpdate) {
                return setOption(applyDefaults(option || {}), notMerge, lazyUpdate);
            };
            return chart;
        };
        window.echarts.__finsightPatched = true;
    }

    patchEchartsInit();

    return {
        applyDefaults: applyDefaults,
        formatMmDd: formatMmDd,
        formatFullDate: formatFullDate,
        daySpan: daySpan,
        axisLabelInterval: axisLabelInterval,
        patchEchartsInit: patchEchartsInit
    };
})();
