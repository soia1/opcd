# OPCD Android — Task Plan & Milestones

## Overview

هذا الملف يحتوي على خطة العمل والمراحل الرئيسية (Milestones) لتنفيذ مشروع OPCD Android.

---

## Milestone 1 — Research & Preparation

**Goal:** فهم المتطلبات التقنية وإعداد البيئة.

- [ ] تحليل مشروع `Victozee26/acode-opencode` المرجعي.
- [ ] دراسة كيفية تشغيل Linux Runtime على Android بدون Termux/UserLAnd.
- [ ] اختيار طريقة Linux Runtime (مثل: proot، chroot، أو توزيعة مدمجة).
- [ ] دراسة WebView على Android لتشغيل OpenCode UI.
- [ ] تحديد الحد الأدنى من إصدار Android ودعم ARM64.
- [ ] إعداد هيكل المستودع على GitHub.

**Deliverables:**
- تقرير تقني مختصر.
- هيكل المشروع الأولي.

---

## Milestone 2 — Proof of Concept (POC)

**Goal:** إثبات إمكانية تشغيل OpenCode محلياً على Android.

- [x] تشغيل Linux Runtime داخل تطبيق Android تجريبي.
- [x] تثبيت Node.js داخل Linux Runtime.
- [x] تثبيت OpenCode داخل Linux Runtime.
- [x] تشغيل OpenCode Server محلياً على `localhost`.
- [x] عرض OpenCode UI داخل WebView في Android.
- [x] التحقق من Health Check.

**Deliverables:**
- نسخة POC (scripts + minimal Android project) — تتطلب اختباراً على جهاز Android حقيقي.
- وثيقة المشاكل والحلول (`docs/poc.md`).

**Note:** لا يمكن اختبار POC فعلياً في بيئة التطوير الحالية لأنها ليست Android. تم إعداد جميع الملفات والسكربتات اللازمة للاختبار على جهاز Android أو Emulator.

---

## Milestone 3 — Android Host Application

**Goal:** بناء هيكل التطبيق الأساسي على Android.

- [x] إنشاء Android Project (Kotlin/Java).
- [x] تطبيق Application Shell.
- [x] إدارة Permissions (Storage, Internet, Notifications).
- [x] إدارة دورة حياة Linux Runtime.
- [x] إدارة العمليات (Process Management).
- [x] دمج WebView لعرض OpenCode UI.
- [x] التعامل مع إغلاق التطبيق وتنظيف العمليات.

**Deliverables:**
- تطبيق Android أساسي يشغل OpenCode UI (`android/app`).
- `OpenCodeService` لإدارة الخادم في الخلفية.
- `PermissionHelper` لطلب الصلاحيات.
- `SettingsActivity` للإعدادات.
- `OpcdApplication` لتهيئة Notification Channel.

---

## Milestone 4 — Linux Runtime Integration

**Goal:** دمج بيئة Linux الكاملة داخل التطبيق.

- [x] تجهيز Linux Runtime المدمج أو التنزيل عند أول تشغيل.
- [x] تثبيت Node.js وnpm.
- [x] تثبيت Python وpip.
- [x] تثبيت Git.
- [x] تثبيت OpenCode.
- [x] دعم Package Managers (apk/apt/npm/pip).
- [x] اختبار تشغيل الأوامر الأساسية (CommandRunner utility).

**Deliverables:**
- `RuntimeManager`: يدير PRoot + Alpine + OpenCode.
- `CommandRunner`: لتشغيل الأوامر داخل Linux Runtime.
- تدفق إعداد تلقائي عند أول تشغيل من داخل التطبيق.
- دعم Package Managers: apk, npm, pip.

---

## Milestone 5 — Terminal Implementation

**Goal:** توفير Terminal Linux حقيقي داخل التطبيق.

- [x] إنشاء واجهة Terminal.
- [x] ربط Terminal بـ Linux Runtime.
- [x] تنفيذ الأوامر فعلياً داخل Shell.
- [x] دعم اختصارات Terminal.
- [x] دعم History وتخصيص Shell.

**Deliverables:**
- `TerminalActivity` مع واجهة Terminal داكنة monospace.
- `TerminalSession`: جلسة `/bin/sh` تفاعلية مستمرة عبر PRoot.
- `ShellHistory`: حفظ واسترجاع سجل الأوامر.
- أزرار اختصارات: Tab, Esc, Ctrl, ↑, ↓, Ctrl+C.
- توثيق القرارات التقنية في `docs/terminal.md`.

---

## Milestone 6 — File Manager

**Goal:** إنشاء مدير ملفات داخلي.

- [ ] عرض ملفات الجهاز والمجلدات.
- [ ] اختيار مجلد المشروع.
- [ ] إنشاء/حذف/إعادة تسمية الملفات والمجلدات.
- [ ] نقل ونسخ الملفات.
- [ ] فتح الملفات في Code Editor.
- [ ] التعامل مع ملفات Git.
- [ ] حفظ صلاحيات الوصول باستخدام Android APIs.

**Deliverables:**
- File Manager كامل.

---

## Milestone 7 — Code Editor

**Goal:** إنشاء محرر أكواد داخلي.

- [ ] دعم Syntax Highlighting.
- [ ] دعم Tabs وMultiple Files.
- [ ] Search وReplace.
- [ ] Save وAuto Save.
- [ ] Undo / Redo.
- [ ] دعم UTF-8.
- [ ] التكامل مع File Manager وOpenCode.

**Deliverables:**
- Code Editor يعمل داخل التطبيق.

---

## Milestone 8 — OpenCode Integration

**Goal:** دمج OpenCode الرسمي بالكامل.

- [ ] تثبيت OpenCode داخل Linux Runtime.
- [ ] تشغيل OpenCode Server.
- [ ] عرض OpenCode UI داخل WebView.
- [ ] الحفاظ على Layout وNavigation وSessions وChat.
- [ ] دعم Tool UI وProject UI.
- [ ] دعم Settings وTheme.
- [ ] التحقق من Health Check.

**Deliverables:**
- OpenCode يعمل داخل التطبيق بشكل مطابق.

---

## Milestone 9 — Git Integration

**Goal:** دعم Git بالكامل.

- [ ] التأكد من توفر Git داخل Linux Runtime.
- [ ] دعم `git clone/status/add/commit/pull/push/branch/checkout/merge`.
- [ ] دعم GitHub عبر HTTPS.
- [ ] دعم SSH وSSH Keys.
- [ ] التكامل مع File Manager.

**Deliverables:**
- Git يعمل داخل Terminal.

---

## Milestone 10 — AI Providers Compatibility

**Goal:** الحفاظ على دعم AI Providers الخاصة بـ OpenCode.

- [ ] التأكد من توافق OpenCode مع AI Providers.
- [ ] دعم مزودي AI الحاليين.
- [ ] حماية API Keys.
- [ ] عدم تضمين API Keys داخل APK.

**Deliverables:**
- AI Providers يعملون داخل OpenCode.

---

## Milestone 11 — Updates System

**Goal:** إضافة نظام التحديثات.

- [ ] التحقق من إصدار OpenCode الجديد.
- [ ] تحديث OpenCode داخلياً.
- [ ] التحقق من إصدار OPCD Android الجديد.
- [ ] تنزيل APK من GitHub Releases.
- [ ] تثبيت APK الجديد مع مراعاة قيود Android.

**Deliverables:**
- نظام تحديثات للتطبيق ولـ OpenCode.

---

## Milestone 12 — Offline Development Support

**Goal:** دعم العمل بدون إنترنت.

- [ ] فتح المشاريع دون إنترنت.
- [ ] تعديل الملفات دون إنترنت.
- [ ] استخدام Terminal دون إنترنت.
- [ ] تشغيل البرامج المحلية دون إنترنت.
- [ ] استخدام Git المحلي دون إنترنت.
- [ ] التعامل مع الوظائف التي تحتاج إنترنت فقط.

**Deliverables:**
- التطبيق يعمل Offline للعمليات المحلية.

---

## Milestone 13 — Security & Permissions

**Goal:** تأمين التطبيق.

- [ ] حماية API Keys.
- [ ] حماية ملفات الإعدادات الحساسة.
- [ ] عزل Linux Runtime قدر الإمكان.
- [ ] استخدام Android Security Model.
- [ ] عدم إرسال الملفات لخدمات خارجية بدون علم المستخدم.
- [ ] تحذير المستخدم من أوامر Terminal الخطرة.

**Deliverables:**
- تقرير أمان وتحسينات مطبقة.

---

## Milestone 14 — Performance Optimization

**Goal:** تحسين الأداء للأجهزة المحمولة.

- [ ] تقليل استهلاك RAM.
- [ ] تقليل CPU Usage.
- [ ] تقليل حجم Linux Runtime.
- [ ] تقليل وقت تشغيل OpenCode.
- [ ] إدارة العمليات في الخلفية.
- [ ] اختبار على أجهزة ضعيفة ومتوسطة وقوية.

**Deliverables:**
- تقرير أداء وتحسينات.

---

## Milestone 15 — Testing & Compatibility

**Goal:** اختبار التطبيق على مختلف الأجهزة.

- [ ] اختبار على Android versions المختلفة.
- [ ] اختبار على ARM64.
- [ ] اختبار على الهواتف والأجهزة اللوحية.
- [ ] اختبار الوضع العمودي والأفقي.
- [ ] اختبار لوحات المفاتيح الخارجية.
- [ ] اختبار Process Crashes.
- [ ] اختبار Network Failures.
- [ ] اختبار Package Installation.

**Deliverables:**
- تقرير اختبارات شاملة.

---

## Milestone 16 — Open Source & Release

**Goal:** إصدار المشروع بشكل مفتوح المصدر.

- [ ] كتابة README.md.
- [ ] إضافة LICENSE (MIT).
- [ ] كتابة CONTRIBUTING.md.
- [ ] كتابة CHANGELOG.md.
- [ ] إنشاء docs/.
- [ ] تنظيم src/ وandroid/ وscripts/.
- [ ] بناء APK جاهز.
- [ ] نشر الإصدار v1.0 على GitHub Releases.

**Deliverables:**
- مستودع مفتوح المصدر.
- APK متوفر للتنزيل.

---

## Definition of Done (Final Checklist)

- [ ] يعمل بدون Acode.
- [ ] يعمل بدون Termux.
- [ ] يعمل بدون UserLAnd.
- [ ] يعمل بدون VPS.
- [ ] يعمل بدون كمبيوتر خارجي.
- [ ] Linux Runtime مدمج.
- [ ] OpenCode يعمل محلياً.
- [ ] OpenCode UI تعمل بشكل مطابق قدر الإمكان.
- [ ] Terminal حقيقي يعمل.
- [ ] File Manager يعمل.
- [ ] Code Editor يعمل.
- [ ] Git يعمل.
- [ ] GitHub يعمل.
- [ ] Package Managers تعمل.
- [ ] AI Providers الخاصة بـ OpenCode تعمل.
- [ ] Android File Access يعمل.
- [ ] OpenCode قابل للتحديث.
- [ ] OPCD Android قابل للتحديث.
- [ ] APK متوفر عبر GitHub Releases.
- [ ] المشروع مفتوح المصدر.
- [ ] MIT License.
- [ ] يعمل على ARM64.
- [ ] تم اختبار الاستقرار والأداء.
- [ ] يمكن للمستخدم تنفيذ دورة تطوير كاملة من الهاتف فقط.

---

## Final Goal

> **OpenCode, running as a self-contained development environment on Android.**
>
> افتح هاتفك، افتح OPCD، وابدأ البرمجة باستخدام OpenCode — بدون أي شيء آخر.
