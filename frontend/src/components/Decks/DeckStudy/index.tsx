import Button from "@/components/ui/Button";
import { selectCardsByDeckId, selectDeckById } from "@/store/decks/module";
import { cn } from "@/utils/classNameMerge";
import { IconArrowLeft, IconArrowRight } from "@tabler/icons-react";
import { clamp, isNil } from "lodash";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useSelector } from "react-redux";

interface Props {
  id?: string;
}

const DeckStudy = ({ id }: Props) => {
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [isFront, setIsFront] = useState(true);
  const router = useRouter();

  const deck = useSelector(!isNil(id) ? selectDeckById(id) : () => undefined);

  const cards = useSelector(
    !isNil(deck?.id) ? selectCardsByDeckId(deck?.id) : () => [],
  );

  useEffect(() => {
    setCurrentCardIndex(0);
  }, [cards.length]);

  if (isNil(deck)) return "Deck is not defined";

  const currentCard = cards.at(currentCardIndex) ?? {
    front: "card not found",
    back: "card not found",
  };

  const incrementCard = (count: number) => {
    setIsFront(true);
    setCurrentCardIndex((prev) => {
      const newIndex = clamp(prev + count, 0, cards.length - 1);
      return newIndex;
    });
  };

  return (
    <div className="flex flex-col gap-2 flex-1 pb-[16px]">
      <div className="flex gap-[20px] items-center">
        <Button
          isIcon
          size="sm"
          className="h-[48px] px-[12px]"
          onClick={() => router.push("/decks")}
        >
          <IconArrowLeft stroke={3} size={24} />
        </Button>
        <h1 className="font-bold text-[40px]">{deck.name}</h1>
      </div>
      <div
        className={cn(
          "flex flex-col rounded-[14px] px-[36px] py-[30px] flex-1 justify-between text-[30px]",
          isFront ? "bg-base-01" : "bg-base-03",
        )}
        onClick={() => setIsFront((p) => !p)}
      >
        <div className="flex justify-between w-full">
          <label className="font-bold text-text-base/50">
            {isFront ? "Question" : "Answer"}
          </label>
          <label className="font-bold text-text-base/50">
            {currentCardIndex + 1} / {cards.length}
          </label>
        </div>
        <h1 className="text-[44px] leading-[4*px] font-bold">
          {isFront ? currentCard.front : currentCard.back}
        </h1>
        <label className="w-full text-center text-text-base/50 text-[24px] font-medium">
          Click to flip
        </label>
      </div>
      <div className="gap-2 flex rounded-[14px]">
        <Button className="flex-1" onClick={() => incrementCard(-1)}>
          <IconArrowLeft stroke={3} />
        </Button>
        <Button
          variant="primary"
          className="flex-1"
          onClick={() => incrementCard(1)}
        >
          <IconArrowRight stroke={3} />
        </Button>
      </div>
    </div>
  );
};

export default DeckStudy;
