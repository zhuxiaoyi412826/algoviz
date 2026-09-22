/**
 * 密码框「查看密码」开关（前台所有密码输入框通用）
 *
 * 页面加载后自动为每一个 input[type="password"] 追加一个「眼睛」按钮：
 *   点击在 明文 / 密文 之间切换，同步切换图标、title 与 aria-label。
 *
 * 实现要点：
 *   1. 不依赖 id / class —— 新增密码框无需改本文件或改调用方；
 *   2. 密码框被包进 position:relative 的容器，按钮绝对定位在右侧；
 *   3. 让位用的 padding-right 走**内联样式**（内联优先级高于各页主题里的
 *      .form-input / body.lamp-on .form-input 等规则，避免图标压住文字）；
 *   4. 按钮 type="button" 且拦截 mousedown，避免误触发表单提交、保持输入框焦点。
 */
(function () {
    'use strict';

    // 眼睛（密文状态，点击后显示密码）
    var ICON_EYE =
        '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" ' +
        'stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
        '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>' +
        '<circle cx="12" cy="12" r="3"></circle></svg>';

    // 眼睛加斜线（明文状态，点击后隐藏密码）
    var ICON_EYE_OFF =
        '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" ' +
        'stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
        '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94">' +
        '</path><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19">' +
        '</path><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"></path>' +
        '<line x1="1" y1="1" x2="23" y2="23"></line></svg>';

    var OPACITY_IDLE = '0.75';
    var OPACITY_HOVER = '1';

    // 固定中性灰：不跟随 input 的 color（login.html 的 开灯/关灯 主题会在加载后切换 input 颜色，
    // 而按钮是 input 的兄弟节点，继承不到），该色在浅色与深色输入框上都有足够对比度
    var ICON_COLOR = '#94a3b8';

    /** 为单个密码框装上看/隐藏开关 */
    function enhance(input) {
        // 已经处理过（避免重复包裹）或已禁用则跳过
        if (input.dataset.pwdToggleReady === '1' || input.disabled) {
            return;
        }
        input.dataset.pwdToggleReady = '1';

        var wrapper = document.createElement('span');
        wrapper.className = 'pwd-toggle-wrap';
        wrapper.style.cssText = 'position:relative;display:block;width:100%;';

        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'pwd-toggle-btn';
        button.innerHTML = ICON_EYE;
        button.style.cssText =
            'position:absolute;right:0.55rem;top:50%;transform:translateY(-50%);' +
            'display:flex;align-items:center;justify-content:center;' +
            'width:1.85rem;height:1.85rem;padding:0;margin:0;border:0;' +
            'background:transparent;cursor:pointer;color:' + ICON_COLOR + ';' +
            'opacity:' + OPACITY_IDLE + ';line-height:1;border-radius:6px;' +
            '-webkit-appearance:none;appearance:none;';
        setLabel(button, false);

        // 点击时不要把焦点从输入框抢走，也不要在 <form> 内触发提交
        button.addEventListener('mousedown', function (event) {
            event.preventDefault();
        });
        button.addEventListener('mouseenter', function () {
            button.style.opacity = OPACITY_HOVER;
        });
        button.addEventListener('mouseleave', function () {
            button.style.opacity = OPACITY_IDLE;
        });
        button.addEventListener('click', function () {
            var showPlain = input.getAttribute('type') === 'password';
            input.setAttribute('type', showPlain ? 'text' : 'password');
            button.innerHTML = showPlain ? ICON_EYE_OFF : ICON_EYE;
            setLabel(button, showPlain);
            // 交还焦点并把光标放到末尾，便于继续输入
            input.focus();
            try {
                var end = input.value.length;
                input.setSelectionRange(end, end);
            } catch (ignored) {
                // 极少数类型不支持选区，忽略
            }
        });

        // 先包裹再插按钮，保持原有 DOM 位置（父节点与相邻兄弟不变）
        input.parentNode.insertBefore(wrapper, input);
        wrapper.appendChild(input);
        wrapper.appendChild(button);

        // 内联 padding-right：给右侧图标让位（优先级最高，不被页面主题覆盖）
        input.style.paddingRight = '2.6rem';
    }

    function setLabel(button, showingPlain) {
        var label = showingPlain ? '隐藏密码' : '查看密码';
        button.setAttribute('title', label);
        button.setAttribute('aria-label', label);
    }

    function init() {
        var inputs = document.querySelectorAll('input[type="password"]');
        Array.prototype.forEach.call(inputs, enhance);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();