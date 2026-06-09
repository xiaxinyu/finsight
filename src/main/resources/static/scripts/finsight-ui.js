/**
 * FinSight shared page interactions: loading, filters, tables, truncation.
 */
var finsightUi = (function ($) {
    'use strict';

    var loadingCount = 0;
    var reportMethods = [
        'consumeReport', 'weekConsumeReport', 'monthConsumeReport',
        'monthIncomeReport', 'monthExpenseReport', 'homeSummary'
    ];

    function chartHosts() {
        return $('#main, .main-chart, .chart-main, #pie, #pieIncome, #barIncomeTop, [data-fs-chart]');
    }

    function ensureChartWrap($host) {
        if (!$host.length) {
            return $();
        }
        if ($host.parent().hasClass('fs-chart-wrap')) {
            return $host.parent();
        }
        $host.wrap('<div class="fs-chart-wrap"></div>');
        var $wrap = $host.parent();
        if (!$wrap.find('.fs-chart-loading').length) {
            $wrap.append('<div class="fs-chart-loading" aria-live="polite">Updating chart...</div>');
        }
        return $wrap;
    }

    function beginLoading(opts) {
        opts = opts || {};
        loadingCount++;
        $('body').addClass('fs-page-loading');
        chartHosts().each(function () {
            ensureChartWrap($(this)).find('.fs-chart-loading').addClass('is-active');
        });
        if (opts.applySelector) {
            $(opts.applySelector).addClass('fs-is-loading');
        } else {
            $('.fs-btn-apply, .fs-btn-primary').addClass('fs-is-loading');
        }
    }

    function endLoading(opts) {
        opts = opts || {};
        loadingCount = Math.max(0, loadingCount - 1);
        if (loadingCount > 0) {
            return;
        }
        $('body').removeClass('fs-page-loading');
        $('.fs-chart-loading').removeClass('is-active');
        $('.fs-is-loading').removeClass('fs-is-loading');
    }

    function wrapAjaxMethod(obj, methodName) {
        if (!obj || typeof obj[methodName] !== 'function' || obj[methodName].__finsightWrapped) {
            return;
        }
        var original = obj[methodName];
        obj[methodName] = function (params, sfn, efn) {
            beginLoading();
            return original.call(obj, params, function (res) {
                try {
                    if (typeof sfn === 'function') {
                        sfn(res);
                    }
                } finally {
                    endLoading();
                }
            }, function (xhr) {
                endLoading();
                if (typeof efn === 'function') {
                    efn(xhr);
                }
            });
        };
        obj[methodName].__finsightWrapped = true;
    }

    function patchReportAjax() {
        if (!window.transaction) {
            return;
        }
        for (var i = 0; i < reportMethods.length; i++) {
            wrapAjaxMethod(window.transaction, reportMethods[i]);
        }
    }

    function formatDate(d) {
        if (typeof window.formatDateMmDdYyyy === 'function') {
            return formatDateMmDdYyyy(d);
        }
        var y = d.getFullYear();
        var m = d.getMonth() + 1;
        var day = d.getDate();
        return (m < 10 ? '0' + m : m) + '/' + (day < 10 ? '0' + day : day) + '/' + y;
    }

    function setDateRange(startSel, endSel, start, end, onChange) {
        try {
            $(startSel).datebox('setValue', start);
            $(endSel).datebox('setValue', end);
        } catch (e) {
            $(startSel).val(start);
            $(endSel).val(end);
        }
        if (typeof onChange === 'function') {
            onChange();
        }
    }

    function initDateShortcuts(config) {
        config = config || {};
        var startSel = config.start || '#dateStart';
        var endSel = config.end || '#dateEnd';
        if (!$(startSel).length || !$(endSel).length) {
            return;
        }
        if ($('.fs-date-shortcuts').length) {
            return;
        }

        var onChange = config.onChange;
        var $bar = $('<span class="fs-date-shortcuts" role="group" aria-label="Date shortcuts"></span>');
        var shortcuts = [
            { label: 'Today', fn: function () {
                var t = new Date();
                var s = formatDate(t);
                setDateRange(startSel, endSel, s, s, onChange);
            }},
            { label: 'This week', fn: function () {
                var now = new Date();
                var day = now.getDay() || 7;
                var monday = new Date(now);
                monday.setDate(now.getDate() - day + 1);
                setDateRange(startSel, endSel, formatDate(monday), formatDate(now), onChange);
            }},
            { label: 'This month', fn: function () {
                var now = new Date();
                var start = new Date(now.getFullYear(), now.getMonth(), 1);
                setDateRange(startSel, endSel, formatDate(start), formatDate(now), onChange);
            }},
            { label: 'Last month', fn: function () {
                var now = new Date();
                var start = new Date(now.getFullYear(), now.getMonth() - 1, 1);
                var end = new Date(now.getFullYear(), now.getMonth(), 0);
                setDateRange(startSel, endSel, formatDate(start), formatDate(end), onChange);
            }}
        ];

        shortcuts.forEach(function (sc) {
            var $btn = $('<a href="javascript:void(0)" class="easyui-linkbutton" data-options="plain:true"></a>');
            $btn.linkbutton({ text: sc.label });
            $btn.on('click', sc.fn);
            $bar.append($btn);
        });

        $(endSel).closest('.filter-item, .item_intervel').after($bar);
        try {
            $.parser.parse($bar);
        } catch (e) { /* ignore */ }
    }

    function bindEnterSubmit(config) {
        config = config || {};
        var selector = config.filterSelector || '.filter-bar, [data-options*="region:\'north\'"]';
        var submit = config.onSubmit;
        if (typeof submit !== 'function') {
            return;
        }
        $(selector).on('keydown', 'input, select', function (e) {
            if (e.key === 'Enter' || e.keyCode === 13) {
                e.preventDefault();
                submit();
            }
        });
    }

    function enhanceTruncate(root) {
        $(root || document).find('.datagrid-cell, .fs-truncate, td, th').each(function () {
            var el = this;
            if (el.scrollWidth > el.clientWidth + 2) {
                var text = $.trim($(el).text());
                if (text && !el.getAttribute('title')) {
                    el.setAttribute('title', text);
                }
            }
        });
    }

    function deltaFormatter(value) {
        var n = Number(value);
        if (isNaN(n)) {
            return '';
        }
        var cls = n < 0 ? 'fs-delta-negative' : (n > 0 ? 'fs-delta-positive' : 'fs-delta-neutral');
        var sign = n > 0 ? '+' : '';
        return '<span class="' + cls + ' fs-money-num">' + sign + n.toFixed(1) + '%</span>';
    }

    function numberOnlyFormatter(value) {
        var n = Number(value);
        if (isNaN(n)) {
            return '';
        }
        return '<span class="fs-money-num">' + n.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '</span>';
    }

    function enhanceDataGrid($grid) {
        if (!$grid || !$grid.length || $grid.data('fsEnhanced')) {
            return;
        }
        $grid.addClass('fs-table');
        var opts = {};
        try {
            opts = $grid.datagrid('options') || {};
        } catch (e) {
            return;
        }

        $grid.datagrid({
            rowStyler: function (index, row) {
                if (row && (row._totalRow || row.month === 'Total' || row.key === 'Total')) {
                    return 'fs-total-row';
                }
            },
            onLoadSuccess: function () {
                enhanceTruncate($grid);
            },
            onSortColumn: function (sort, order) {
                $grid.find('.datagrid-header .datagrid-cell').removeClass('fs-sort-active');
                var $col = $grid.find('.datagrid-header td[field="' + sort + '"] .datagrid-cell');
                $col.addClass('fs-sort-active');
                var arrow = order === 'desc' ? '↓' : '↑';
                $col.find('.fs-sort-indicator').remove();
                $col.append('<span class="fs-sort-indicator">' + arrow + '</span>');
            }
        });

        if (opts.remoteSort === undefined) {
            try {
                $grid.datagrid({ remoteSort: false });
            } catch (e) { /* ignore */ }
        }

        $grid.data('fsEnhanced', true);
    }

    function wrapChartCards() {
        chartHosts().each(function () {
            var $host = $(this);
            ensureChartWrap($host);
            if (!$host.hasClass('fs-chart-host')) {
                $host.addClass('fs-chart-host');
            }
        });
        $('.chart-card, .fs-chart-card').each(function () {
            if (!$(this).hasClass('fs-chart-card')) {
                $(this).addClass('fs-chart-card');
            }
        });
    }

    function detectReportPage() {
        if (typeof window.echarts !== 'undefined' && chartHosts().length) {
            $('body').addClass('fs-report-page');
            return true;
        }
        return false;
    }

    function initReportPage(config) {
        config = config || {};
        detectReportPage();
        patchReportAjax();
        wrapChartCards();
        initDateShortcuts({
            start: config.startDate || '#dateStart',
            end: config.endDate || '#dateEnd',
            onChange: config.onSubmit
        });
        bindEnterSubmit({
            filterSelector: config.filterSelector,
            onSubmit: config.onSubmit || function () {
                if (typeof window.statistic === 'function') {
                    window.statistic();
                } else if (typeof window.scheduleStatistic === 'function') {
                    window.scheduleStatistic();
                } else if (typeof window.compareYears === 'function') {
                    window.compareYears();
                }
            }
        });
        $('.easyui-datagrid').each(function () {
            enhanceDataGrid($(this));
        });
        enhanceTruncate(document.body);
    }

    $(function () {
        if (window.finsightCharts && finsightCharts.patchEchartsInit) {
            finsightCharts.patchEchartsInit();
        }
        initReportPage();
        $(document).on('DOMNodeInserted', function () {
            enhanceTruncate(document.body);
        });
    });

    return {
        beginLoading: beginLoading,
        endLoading: endLoading,
        initReportPage: initReportPage,
        initDateShortcuts: initDateShortcuts,
        enhanceDataGrid: enhanceDataGrid,
        enhanceTruncate: enhanceTruncate,
        deltaFormatter: deltaFormatter,
        numberOnlyFormatter: numberOnlyFormatter,
        patchReportAjax: patchReportAjax
    };
})(jQuery);
