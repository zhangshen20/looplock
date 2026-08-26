from google.adk.agents import Agent

from looplock_agent.config import Settings
from looplock_agent.instructions import AGENT_INSTRUCTION
from looplock_agent.models import AgentDraft


def create_agent(settings: Settings) -> Agent:
    return Agent(
        name="looplock_classifier",
        model=settings.model_name,
        description="Classifies only LoopLock's harmless Android demo fixture metadata.",
        instruction=AGENT_INSTRUCTION,
        output_schema=AgentDraft,
        include_contents="none",
        # ADK 2.7 requires a root LlmAgent to use chat or task mode. Each API
        # request still gets a fresh session and includes no prior contents.
        mode="chat",
    )
