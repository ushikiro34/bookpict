import React from "react";
import { motion } from "motion/react";

/**
 * SearchBooks Discovery Icon
 * 
 * Represents the process of searching and discovering knowledge.
 * Features a magnifying glass overlaying two books.
 */
const SearchBooksIcon = ({
    size = 64,
    primaryColor = "#2d3e50",
    accentColor = "#f97316",
}) => {
    return (
        <div
            className="relative flex items-center justify-center"
            style={{ width: size, height: size }}
        >
            {/* 1. Scrambled Books - Now 2 books, rendered first (behind) */}
            <div
                className="absolute"
                style={{
                    width: size * 0.45,
                    height: size * 0.45,
                    top: "18%",
                    left: "18%",
                    zIndex: 0
                }}
            >
                <div className="relative w-full h-full">
                    {/* Book 1 - Orange */}
                    <motion.div
                        initial={{ rotate: -20, opacity: 0 }}
                        animate={{ rotate: -15, opacity: 1 }}
                        transition={{ delay: 0.2 }}
                        className="absolute top-0 left-0 w-[70%] h-[90%] rounded-sm shadow-sm"
                        style={{
                            backgroundColor: accentColor,
                            border: "1px solid white",
                        }}
                    />
                    {/* Book 2 - Navy/Primary */}
                    <motion.div
                        initial={{ rotate: 15, opacity: 0 }}
                        animate={{ rotate: 25, opacity: 1 }}
                        transition={{ delay: 0.4 }}
                        className="absolute top-1 left-3 w-[70%] h-[90%] rounded-sm shadow-sm"
                        style={{
                            backgroundColor: primaryColor,
                            border: "1px solid white",
                        }}
                    />
                </div>
            </div>

            {/* 2. Magnifying Glass Frame - Rendered second (in front) */}
            <svg
                width={size}
                height={size}
                viewBox="0 0 24 24"
                fill="none"
                stroke={primaryColor}
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="overflow-visible relative"
                style={{ zIndex: 10 }}
            >
                {/* The Lens Circle (Outer) */}
                <circle cx="11" cy="11" r="8.5" strokeWidth="2.5" />

                {/* Transparent glass area (optional, but keep it clear) */}
                <circle cx="11" cy="11" r="7.25" fill="white" fillOpacity="0.1" stroke="none" />

                {/* The Handle */}
                <line
                    x1="21"
                    y1="21"
                    x2="17.2"
                    y2="17.2"
                    strokeWidth="3.5"
                />
            </svg>
        </div>
    );
};

export default SearchBooksIcon;
