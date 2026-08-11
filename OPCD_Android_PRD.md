# PRD — OPCD Android

## 1. معلومات المشروع

- **اسم المشروع:** OPCD Android
- **المنصة:** Android
- **الترخيص:** MIT License
- **المصدر:** Open Source على GitHub
- **التوزيع:** APK عبر GitHub Releases
- **التحديث:** تلقائي
- **المشروع المرجعي:** `Victozee26/acode-opencode`

## 2. الرؤية

إنشاء تطبيق Android مستقل يوفر **OpenCode على الهاتف** دون الحاجة إلى Acode أو Termux أو UserLAnd أو Ubuntu خارجي أو VPS أو كمبيوتر أو أي تطبيق مساعد.

يجب أن يكون التطبيق بيئة تطوير كاملة ومستقلة على Android تعتمد على الهاتف فقط.

## 3. المبدأ الأساسي

المشروع هو تحويل فكرة `acode-opencode` من Acode Plugin إلى Android Application مستقل.

```text
acode-opencode
       ↓
إزالة اعتماد Acode
       ↓
استبدال Acode Runtime ببيئة Android مستقلة
       ↓
OPCD Android
       ↓
OpenCode
```

يجب الحفاظ على تجربة OpenCode ووظائفه ومنطقه قدر الإمكان.

## 4. البيئة المستهدفة

يحتوي التطبيق على Linux Runtime مدمج، ويشمل البيئة اللازمة لتشغيل OpenCode وأدوات التطوير.

```text
┌──────────────────────────────┐
│         OPCD Android         │
├──────────────────────────────┤
│        OpenCode UI           │
├──────────────────────────────┤
│ File Manager                 │
│ Code Editor                  │
│ Terminal                     │
├──────────────────────────────┤
│ Linux Runtime                │
│ Node.js │ npm │ Git          │
│ Python │ pip │ OpenCode      │
└──────────────────────────────┘
```

لا يعتمد التطبيق على تطبيقات خارجية لتوفير بيئة Linux.

## 5. OpenCode

يجب تشغيل OpenCode محلياً داخل الجهاز.

```text
Application Start
       ↓
Check Linux Environment
       ↓
Check Node.js
       ↓
Check OpenCode
       ↓
Install / Update if required
       ↓
Start OpenCode Server
       ↓
Health Check
       ↓
Load OpenCode UI
```

يجب الحفاظ على مفاهيم المشروع المرجعي: Check, Install, Serve, Health, Render.

## 6. واجهة OpenCode

يجب أن تكون واجهة OpenCode مطابقة لواجهة OpenCode الأصلية بنسبة 100% قدر الإمكان، وليس إنشاء واجهة بديلة مستوحاة منها.

يتم تشغيل OpenCode محلياً ثم عرض واجهته الرسمية داخل التطبيق.

يجب الحفاظ على Layout وNavigation وSessions وChat وTool UI وProject UI وSettings وTheme وInteractions وKeyboard behavior وOpenCode workflows وفقاً للإصدار الحالي.

## 7. File Manager

يحتوي التطبيق على File Manager داخلي كامل يدعم:

- الوصول إلى ملفات الجهاز والمجلدات.
- اختيار مجلد المشروع.
- إنشاء وحذف وإعادة تسمية الملفات والمجلدات.
- نقل ونسخ الملفات.
- فتح المشاريع والملفات.
- التعامل مع ملفات Git.
- فتح الملفات في Code Editor.

يجب أن يستطيع المستخدم منح التطبيق صلاحية الوصول إلى الملفات والمجلدات المطلوبة.

## 8. Code Editor

محرر ملفات داخلي يدعم:

- Syntax Highlighting
- البحث وReplace
- Multiple Files
- Tabs
- Save وAuto Save
- Undo / Redo
- UTF-8
- الملفات الكبيرة
- التكامل مع File Manager وOpenCode

## 9. Linux Terminal

يجب أن يحتوي OPCD Android على Linux Terminal حقيقي، وليس Terminal شكلياً.

مثال:

```bash
pwd
ls
cd project
git status
python app.py
npm install
npm run dev
```

يجب تنفيذ الأوامر فعلياً داخل Linux Runtime.

## 10. Linux Environment

تكون بيئة Linux جزءاً من التطبيق نفسه.

لا يحتاج المستخدم إلى Termux أو UserLAnd أو Acode أو proot-distro خارجي أو Ubuntu خارجي.

يمكن تنزيل مكونات البيئة عند أول تشغيل إذا كان تضمينها بالكامل داخل APK غير عملي بسبب الحجم، لكن بعد التهيئة تكون البيئة مستقلة داخل OPCD Android.

## 11. Package Management

يجب أن يستطيع المستخدم تثبيت الحزم والأدوات من داخل Terminal، مثل:

```bash
apk
apt
npm
pip
```

حسب Linux Runtime المستخدم.

## 12. Development Environment

يجب أن يستطيع OPCD Android تشغيل المشاريع فعلياً على الهاتف، بما في ذلك:

- Python
- Node.js
- JavaScript
- TypeScript
- HTML
- CSS
- Shell

مع إمكانية تثبيت أدوات ولغات إضافية.

## 13. Git

يجب توفير Git داخل Linux Runtime ودعم:

```bash
git clone
git status
git add
git commit
git pull
git push
git branch
git checkout
git merge
```

ويجب دعم GitHub عبر HTTPS وSSH وSSH keys.

## 14. AI Providers

يجب الحفاظ على نظام AI الخاص بـ OpenCode وعدم إنشاء نظام AI مستقل إذا كانت الوظيفة موجودة في OpenCode.

```text
OPCD Android
      ↓
OpenCode
      ↓
AI Provider
      ↓
Model
```

يجب الحفاظ على توافق التطبيق مع مزودي ونماذج OpenCode الحالية والمستقبلية قدر الإمكان.

## 15. استقلالية التطبيق

هذه متطلبات أساسية:

```text
Acode        ❌
Termux       ❌
UserLAnd     ❌
Ubuntu App   ❌
VPS          ❌
PC           ❌
External SSH ❌
```

كل ما يحتاجه المستخدم لتشغيل OPCD Android يجب أن يكون داخل التطبيق أو يتم توفيره من خلال التطبيق نفسه.

## 16. الإنترنت

لا يحتاج التطبيق إلى Server خارجي لتشغيل البيئة.

يستخدم الإنترنت عند الحاجة إلى:

- AI Providers
- تنزيل OpenCode
- تحديث OpenCode
- تنزيل Linux packages
- GitHub
- Git clone / pull / push
- تحديث OPCD Android

أما العمليات المحلية فتعمل على الهاتف.

## 17. Offline Development

عند عدم توفر الإنترنت يجب أن يستطيع المستخدم فتح المشاريع وتعديل الملفات واستخدام Terminal وتشغيل البرامج المحلية واستخدام Git المحلي.

الوظائف التي تعتمد على الإنترنت فقط هي التي تتوقف.

## 18. OpenCode Updates

يجب أن يكون OpenCode قابلاً للتحديث بشكل مستقل عن التطبيق.

```text
Installed OpenCode
        ↓
Check Latest Version
        ↓
New Version?
     /       \
   Yes        No
    ↓          ↓
 Update      Continue
```

## 19. OPCD Android Updates

يجب دعم التحديث التلقائي للتطبيق نفسه من GitHub Releases.

```text
OPCD Android v1.0
       ↓
GitHub Releases
       ↓
OPCD Android v1.1
       ↓
Update Detection
       ↓
Download
       ↓
Install
```

مع مراعاة قيود Android المتعلقة بتثبيت APK من خارج Google Play.

## 20. Open Source

المشروع بالكامل مفتوح المصدر، ويجب أن يحتوي المستودع على:

```text
README.md
LICENSE
CONTRIBUTING.md
CHANGELOG.md
PRD.md
docs/
src/
android/
scripts/
```

ويتم توفير APK جاهز في GitHub Releases.

## 21. License

المشروع يستخدم MIT License، مع احترام تراخيص جميع المكونات الخارجية.

## 22. Architecture

```text
                    OPCD Android
                         │
          ┌──────────────┴──────────────┐
          │                             │
 Android Native Layer           Linux Runtime
          │                             │
          │                    ┌────────┴────────┐
          │                    │                 │
          │                 Node.js           Tools
          │                    │
          │                 OpenCode
          │                    │
          └──────────────► Local Server
                               │
                               ▼
                          OpenCode UI
```

### Android Layer

مسؤول عن:

- Application lifecycle
- Android permissions
- Storage
- Notifications
- Updates
- Linux Runtime lifecycle
- Process management
- WebView/UI integration
- File access

### Linux Layer

مسؤول عن:

- Shell
- Node.js
- npm
- Python
- pip
- Git
- OpenCode
- User-installed packages

### OpenCode Layer

مسؤول عن:

- AI Agent
- Coding
- Models
- Providers
- Sessions
- Tools
- Projects
- OpenCode UI

## 23. File Access & Permissions

يجب استخدام آليات Android الرسمية للوصول إلى الملفات، والسماح للمستخدم باختيار مجلدات المشاريع وحفظ صلاحيات الوصول إليها وتمكين OpenCode وTerminal من العمل على المشاريع المصرح بها.

## 24. Security

يجب:

- حماية API Keys.
- عدم تضمين API Keys داخل APK.
- عدم إرسال الملفات إلى خدمات خارجية بدون علم المستخدم.
- عزل Linux Runtime قدر الإمكان.
- توضيح أن أوامر Terminal يمكنها تعديل وحذف الملفات.
- استخدام Android security model قدر الإمكان.
- حماية ملفات الإعدادات الحساسة.

## 25. Performance

يجب تصميم التطبيق للأجهزة المحمولة مع:

- استهلاك RAM منخفض قدر الإمكان.
- تقليل CPU usage.
- تقليل حجم Linux Runtime.
- تقليل وقت تشغيل OpenCode.
- عدم إبقاء العمليات غير الضرورية تعمل بالخلفية.
- إدارة العمليات عند إغلاق التطبيق.
- التعامل مع الأجهزة محدودة الموارد.

## 26. Compatibility

يجب استهداف أجهزة Android الحديثة مع التركيز على:

- ARM64
- الهواتف
- Tablets
- شاشات مختلفة
- الوضع العمودي والأفقي
- لوحات المفاتيح الخارجية

ويجب اختبار التطبيق على أجهزة ضعيفة ومتوسطة وقوية.

## 27. User Flow

### أول تشغيل

```text
Install OPCD Android
        ↓
Open App
        ↓
Initial Setup
        ↓
Prepare Linux Runtime
        ↓
Prepare Node.js
        ↓
Install/Prepare OpenCode
        ↓
Start OpenCode
        ↓
Health Check
        ↓
Open OpenCode
```

### التشغيلات اللاحقة

```text
Open OPCD
    ↓
Check Runtime
    ↓
Check OpenCode
    ↓
Start Server
    ↓
Health Check
    ↓
Open OpenCode
```

## 28. Project Workflow

```text
Open OPCD
    ↓
File Manager
    ↓
Select Project
    ↓
Open OpenCode
    ↓
AI works on Project
    ↓
Edit Files
    ↓
Run Terminal Commands
    ↓
Run Project
    ↓
Git Commit
    ↓
Git Push
```

## 29. Non-Goals

لا يهدف المشروع إلى:

- إنشاء AI coding agent جديد.
- استبدال OpenCode.
- إنشاء نظام AI خاص.
- الاعتماد على Acode.
- الاعتماد على Termux.
- الاعتماد على UserLAnd.
- الاعتماد على VPS.
- إنشاء واجهة مختلفة عن OpenCode.

الهدف هو جعل OpenCode يعمل كبيئة تطوير مستقلة على Android.

## 30. Core Principle

> **Do not rebuild OpenCode from scratch.**

يجب استخدام OpenCode نفسه قدر الإمكان.

```text
OpenCode
    +
Android Host
    +
Embedded Linux Runtime
    +
File Access
    +
Code Editor
    +
Linux Terminal
    +
Git
    +
Automatic Updates
    =
OPCD Android
```

## 31. مراحل التطوير

### Phase 1 — Proof of Concept

إثبات تشغيل:

```text
Android
   ↓
Embedded Linux
   ↓
Node.js
   ↓
OpenCode
   ↓
localhost
```

### Phase 2 — Android Host

إضافة:

- Application shell
- Linux lifecycle
- Permissions
- Storage access
- Process management

### Phase 3 — OpenCode Integration

دمج OpenCode الرسمي وتشغيل واجهته.

### Phase 4 — Development Environment

إضافة:

- Terminal
- File Manager
- Code Editor
- Git
- Package managers

### Phase 5 — Updates

إضافة:

- OPCD updater
- OpenCode updater
- GitHub Releases

### Phase 6 — Production

اختبار:

- Android versions
- ARM64
- RAM usage
- CPU usage
- Storage
- Permissions
- Process crashes
- Network failures
- Package installation
- OpenCode compatibility

ثم إصدار:

```text
OPCD Android v1.0
```

## 32. Definition of Done

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

## Final Product

**OPCD Android**

> OpenCode, running as a self-contained development environment on Android.

```text
┌─────────────────────────────────────┐
│             OPCD Android            │
├─────────────────────────────────────┤
│                                     │
│              OpenCode               │
│          AI Coding Agent + UI       │
│                                     │
├─────────────────────────────────────┤
│ File Manager │ Editor │ Terminal    │
├─────────────────────────────────────┤
│        Embedded Linux Runtime       │
│ Node.js │ Python │ Git │ npm │ pip  │
└─────────────────────────────────────┘
```

**الهدف النهائي:** افتح هاتفك، افتح OPCD، وابدأ البرمجة باستخدام OpenCode — بدون أي شيء آخر.
