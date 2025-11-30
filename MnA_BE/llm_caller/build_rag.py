import glob
import os
from langchain_community.document_loaders import PyPDFLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings
from dotenv import load_dotenv

load_dotenv()

PDF_DIR = "rag_pdf"
pdf_files = glob.glob(os.path.join(PDF_DIR, "*.pdf"))
print("Found PDFs:")
for f in pdf_files:
    print(" -", f)

all_docs = []
for pdf_path in pdf_files:
    loader = PyPDFLoader(pdf_path)
    docs = loader.load()
    for d in docs:
        d.metadata["source"] = os.path.basename(pdf_path)
    all_docs.extend(docs)

print(f"총 로드된 페이지 수: {len(all_docs)}")

text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=1000,
    chunk_overlap=200,
    length_function=len,
)

split_docs = text_splitter.split_documents(all_docs)
print(f"청크 개수: {len(split_docs)}")

embeddings = OpenAIEmbeddings()
VECTOR_DIR = "./book_chroma_db"

vectordb = Chroma.from_documents(
    documents=split_docs,
    embedding=embeddings,
    persist_directory=VECTOR_DIR,
)

print(f"Chroma DB 생성 완료, 디렉토리: {VECTOR_DIR}")
