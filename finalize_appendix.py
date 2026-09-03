# -*- coding: utf-8 -*-
from pathlib import Path
from docx import Document
from docx.shared import Inches, Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING

base = Path(r"C:\Users\hesh2\OneDrive\Desktop\subway game")
report_path = base / "JAVA REPORT (1)_merged.docx"
source_java = base / "SimpleRunnerGame.java"

if not report_path.exists():
    raise FileNotFoundError(report_path)

doc = Document(str(report_path))

# Set default formatting across the report
for p in doc.paragraphs:
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
    for r in p.runs:
        r.font.size = Pt(12)

# Append clean appendix at the end

def add_bold_heading(text):
    p = doc.add_paragraph()
    run = p.add_run(text)
    run.bold = True
    run.font.size = Pt(12)
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
    return p

# A. GitHub repository
add_bold_heading("APPENDIX A: GitHub Repository and Project Structure")
p = doc.add_paragraph()
p.add_run("GitHub Link: ")
run = p.add_run("https://github.com/hareshbharadwaj/2DJavaGame-Subway-Running-.git")
run.font.size = Pt(12)
p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE

p = doc.add_paragraph("Project Folder Structure:")
for r in p.runs:
    r.font.size = Pt(12)
p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
for line in [
    "2DJavaGame-Subway-Running-/",
    "|-- assets/",
    "|   |-- collectibles/",
    "|   |-- environment/",
    "|   |-- obstacles/",
    "|   |-- player/",
    "|   |-- ui/",
    "|-- sounds/",
    "|-- db.properties",
    "|-- DbManager.java",
    "|-- PlayerInfo.java",
    "|-- SessionManager.java",
    "|-- SimpleRunnerGame.java",
    "|-- SoundManager.java",
    "|-- README.md",
    "|-- player.txt",
    "|-- .git/",
]:
    p = doc.add_paragraph(line)
    for r in p.runs:
        r.font.size = Pt(12)
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE

# B. Assets and screenshots
add_bold_heading("APPENDIX B: Assets Used and Screenshots")

p = add_bold_heading("B.1 Assets Used")
# Asset table with image placeholders (when files exist)
asset_table = doc.add_table(rows=1, cols=3)
asset_table.rows[0].cells[0].text = "Asset Type"
asset_table.rows[0].cells[1].text = "Asset Image"
asset_table.rows[0].cells[2].text = "Description"
rows = [
    ("Environment", base / "assets" / "environment" / "background.png", "Game background"),
    ("Road", base / "assets" / "environment" / "road (1).png", "Road texture"),
    ("Player", base / "assets" / "player" / "player_run (2).png", "Player sprite"),
    ("Coin", base / "assets" / "collectibles" / "coin1.png", "Coin collectible"),
    ("Obstacle", base / "assets" / "obstacles" / "barrier (1).png", "Obstacle object"),
    ("Power-up", base / "assets" / "ui" / "shield (1).png", "Shield power-up"),
]
for asset_type, img_path, desc in rows:
    row = asset_table.add_row().cells
    row[0].text = asset_type
    row[2].text = desc
    cell = row[1]
    cell.text = ""
    try:
        pcell = cell.paragraphs[0]
        pcell.alignment = WD_ALIGN_PARAGRAPH.CENTER
        pcell.add_run().add_picture(str(img_path), width=Inches(1.1))
    except Exception:
        cell.text = "Image file missing"
    for c in row:
        for para in c.paragraphs:
            para.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
            for r in para.runs:
                r.font.size = Pt(12)

p = add_bold_heading("B.2 Screenshot Upload Space")
shot_table = doc.add_table(rows=1, cols=2)
shot_table.rows[0].cells[0].text = "Figure"
shot_table.rows[0].cells[1].text = "Screenshot Upload Space"
for fig, desc in [
    ("Figure B.1: Home Page", "Home page screenshot to insert here"),
    ("Figure B.2: Game Page", "Game page screenshot to insert here"),
    ("Figure B.3: Leaderboard Page", "Leaderboard page screenshot to insert here"),
]:
    row = shot_table.add_row().cells
    row[0].text = fig
    row[1].text = desc
    for c in row:
        for para in c.paragraphs:
            para.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
            for r in para.runs:
                r.font.size = Pt(12)

# C. Source code snippet pages
add_bold_heading("APPENDIX C: Source Code Snippet")
if source_java.exists():
    code_lines = source_java.read_text(encoding='utf-8', errors='ignore').splitlines()
    chunks = [code_lines[0:90], code_lines[90:180], code_lines[180:270]]
    for idx, chunk in enumerate(chunks, start=1):
        if idx > 1:
            doc.add_page_break()
        p = doc.add_paragraph()
        run = p.add_run(f"Code Snippet Page {idx}")
        run.bold = True
        run.font.size = Pt(12)
        p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
        code_para = doc.add_paragraph()
        code_run = code_para.add_run("\n".join(chunk))
        code_run.font.name = "Consolas"
        code_run.font.size = Pt(10)
        code_para.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
        code_para.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.LEFT

p = doc.add_paragraph("Note: This appendix uses 1.5 line spacing and font size 12 throughout.")
for r in p.runs:
    r.font.size = Pt(12)
p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE

doc.save(str(report_path))
print("APPENDIX_FINALIZED")
print("tables:", len(doc.tables))
