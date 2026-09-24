package com.example.data

import com.example.model.DocumentTemplate
import com.example.model.KashidaLevel
import com.example.model.MarginUnit
import com.example.model.PageMargins
import com.example.model.TextAlignOption
import kotlinx.coroutines.flow.Flow

class DocumentRepository(
    private val documentDao: DocumentDao
) {
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    fun getDocumentById(id: Long): Flow<DocumentEntity?> = documentDao.getDocumentById(id)

    suspend fun getDocumentSnapshot(id: Long): DocumentEntity? = documentDao.getDocumentSnapshot(id)

    suspend fun getLatestDocument(): DocumentEntity? = documentDao.getLatestDocument()

    suspend fun saveDocument(doc: DocumentEntity): Long {
        return documentDao.saveDocument(doc)
    }

    suspend fun deleteDocument(id: Long) {
        documentDao.deleteDocumentById(id)
    }

    suspend fun duplicateDocument(id: Long): Long? {
        val original = documentDao.getDocumentSnapshot(id) ?: return null
        val copy = original.copy(
            id = 0,
            title = "${original.title} (نسخة)",
            updatedAt = System.currentTimeMillis()
        )
        return documentDao.saveDocument(copy)
    }

    /**
     * Built-in rich templates for Arabic users.
     */
    fun getTemplates(): List<DocumentTemplate> {
        return listOf(
            DocumentTemplate(
                id = "religious",
                titleAr = "نص ديني / قرآني",
                descriptionAr = "خط أميري كلاسيكي مع كشيدة متوازنة وخلفية ورق قرطاس وهوامش رحبة",
                categoryAr = "نصوص دينية وشريفة",
                sampleText = """بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ
الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۝ الرَّحْمَٰنِ الرَّحِيمِ ۝ مَالِكِ يَوْمِ الدِّينِ ۝ إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ۝ اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ۝ صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ""",
                fontId = "amiri",
                fontSizePt = 22f,
                textColor = androidx.compose.ui.graphics.Color(0xFF2D1500),
                pageColor = androidx.compose.ui.graphics.Color(0xFFFAF4E8),
                align = TextAlignOption.JUSTIFY,
                kashidaEnabled = true,
                kashidaLevel = KashidaLevel.MEDIUM,
                margins = PageMargins(25f, 25f, 25f, 25f, MarginUnit.MILLIMETER)
            ),
            DocumentTemplate(
                id = "poetry",
                titleAr = "نص شعري / قصيدة عمودية",
                descriptionAr = "خط الرقعة الفني في توزيع شطري متناسق مع كشيدة رشيقة وهوامش فسيحة",
                categoryAr = "شعر وأدب",
                sampleText = """وُلِـدَ الـهُـدى فَـالكائِناتُ ضِياءُ ... وَفَـمُ الـزَمـانِ تَـبَـسُّـمٌ وَثَناءُ
الـروحُ وَالـمَـلَأُ الـمَلائِكُ حَولَهُ ... لِـلـديـنِ وَالـدُنـيـا بِـهِ بُشَراءُ
وَالـعَـرشُ يَزـهو وَالحَظـيرَةُ تَزدَهي ... وَالـمُـنـتَهى وَالسِدرَةُ العَصماءُ
يا خَـيـرَ مَن جاءَ الوُجودَ تَحِيَّةً ... مِـن مُرسَلينَ إِلى هُداكَ بَكـاءُ""",
                fontId = "aref_ruqaa",
                fontSizePt = 20f,
                textColor = androidx.compose.ui.graphics.Color(0xFF1B2A26),
                pageColor = androidx.compose.ui.graphics.Color(0xFFFDFBF7),
                align = TextAlignOption.CENTER,
                kashidaEnabled = true,
                kashidaLevel = KashidaLevel.LIGHT,
                margins = PageMargins(30f, 30f, 20f, 20f, MarginUnit.MILLIMETER)
            ),
            DocumentTemplate(
                id = "formal_letter",
                titleAr = "رسالة رسمية / خطاب إداري",
                descriptionAr = "خط كايرو الهندسي الأنيق بضبط كامل دقيق وكشيدة خفيفة تناسب المخاطبات",
                categoryAr = "مراسلات إدارية",
                sampleText = """سعادة المدير العام المحترم،
السلام عليكم ورحمة الله وبركاته، أما بعد:

يطيب لي أن أرفع إلى عنايتكم هذا التقرير الموجز حول سير العمل في المشاريع الحالية، مشيرًا إلى أن فرق التطوير قد أنجزت المرحلة الأولى وفق الخطة الزمنية المعتمدة والمعايير المهنية المتبعة.

نرجو التفضل بالاطلاع والتوجيه بما ترونه مناسبًا لاعتماد المرحلة القادمة.

وتفضلوا بقبول وافر التحية والتقدير.""",
                fontId = "cairo",
                fontSizePt = 16f,
                textColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                pageColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                align = TextAlignOption.JUSTIFY,
                kashidaEnabled = true,
                kashidaLevel = KashidaLevel.LIGHT,
                margins = PageMargins(25f, 20f, 25f, 20f, MarginUnit.MILLIMETER)
            ),
            DocumentTemplate(
                id = "article",
                titleAr = "مقال صحفي / تدوينة فكرية",
                descriptionAr = "خط تجوال العصري المريح للقراءة على خلفية مريحة للعين مع كشيدة خفيفة",
                categoryAr = "مقالات ونشر",
                sampleText = """اللغة العربية وجماليات الحرف في العصر الرقمي

تعتبر الكتابة العربية واحدة من أثرى الفنون البصرية في الحضارة الإنسانية، حيث لا يقتصر الحرف العربي على كونه أداة لتسجيل الأفكار فحسب، بل هو كائن حي ينبض بالحركة والانسياب.

وتأتي تقنية الكشيدة (المد والتطويل) كواحدة من أبرز خصائص الخط العربي التي تمنحه مرونة فائقة في ضبط الأسطر وتحقيق التوازن البصري والانسجام الجمالي في كل صفحة.""",
                fontId = "tajawal",
                fontSizePt = 17f,
                textColor = androidx.compose.ui.graphics.Color(0xFF0F172A),
                pageColor = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
                align = TextAlignOption.RIGHT,
                kashidaEnabled = true,
                kashidaLevel = KashidaLevel.LIGHT,
                margins = PageMargins(20f, 20f, 20f, 20f, MarginUnit.MILLIMETER)
            ),
            DocumentTemplate(
                id = "book_chapter",
                titleAr = "مفتتح فصل روائي / كتاب",
                descriptionAr = "تنضيد كلاسيكي لصفحات الكتب والروايات بخط أميري وهوامش طباعية متوازنة وكشيدة متوسطة",
                categoryAr = "نشر مكتبي وكتب",
                sampleText = """الفصل الأول: هبوب الريح في الوادي القديم

كان المساء يهبط ببطء فوق تلال المدينة العتيقة، بينما تنساب أسراب الطيور نحو الواحات البعيدة. وقف الشيخ متأملاً الأفق الممتد، وفي عينيه وميض من ذكريات مضت وقصص ترويها الرياح جيلاً بعد جيل.

لم تكن الرحلة سهلة، لكن العزيمة التي سكنت قلوب السائرين جعلت الصخور تلين تحت وطأة خطاهم، حتى بلغت القافلة مشارف المستقر قبل مغيب الشمس.""",
                fontId = "amiri",
                fontSizePt = 17f,
                textColor = androidx.compose.ui.graphics.Color(0xFF18181B),
                pageColor = androidx.compose.ui.graphics.Color(0xFFFAF8F5),
                align = TextAlignOption.JUSTIFY,
                kashidaEnabled = true,
                kashidaLevel = KashidaLevel.MEDIUM,
                margins = PageMargins(25f, 25f, 22f, 20f, MarginUnit.MILLIMETER)
            ),
            DocumentTemplate(
                id = "academic_research",
                titleAr = "بحث علمي / دراسة أكاديمية",
                descriptionAr = "تنسيق رصين للأبحاث والأوراق المحكمة بهوامش نظامية وضبط كامل دقيق",
                categoryAr = "أبحاث ودراسات",
                sampleText = """الملخص التنفيذي للدراسة

تهدف هذه الدراسة إلى استقصاء أثر التنضيد الرقمي على سرعة الاستيعاب القرائي للنصوص العربية الممدودة، ومقارنة معدلات القراءة بين الأنماط المضبوطة بالمسافات البيضاء والأنماط المعتمدة على الكشيدة المحسوبة هندسياً.

وقد خلص البحث إلى أن الضبط المتوازن يحقق راحة بصرية فائقة ويقلل من تشتت النظر أثناء القراءة المتواصلة في الصفحات الطويلة والمراجع الأكاديمية.""",
                fontId = "amiri",
                fontSizePt = 15f,
                textColor = androidx.compose.ui.graphics.Color(0xFF09090B),
                pageColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                align = TextAlignOption.JUSTIFY,
                kashidaEnabled = true,
                kashidaLevel = KashidaLevel.LIGHT,
                margins = PageMargins(25f, 25f, 25f, 25f, MarginUnit.MILLIMETER)
            )
        )
    }
}
