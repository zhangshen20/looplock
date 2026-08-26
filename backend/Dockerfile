FROM python:3.13-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PATH="/app/.venv/bin:$PATH"

RUN python -m pip install --no-cache-dir uv==0.10.7
WORKDIR /app

COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project

COPY src ./src
RUN uv sync --frozen --no-dev

RUN useradd --create-home --uid 10001 looplock && chown -R looplock:looplock /app
USER 10001

EXPOSE 8080
CMD ["uvicorn", "looplock_agent.main:app", "--host", "0.0.0.0", "--port", "8080", "--no-access-log"]
