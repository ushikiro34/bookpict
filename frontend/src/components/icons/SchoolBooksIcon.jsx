import React from "react";
import { motion } from "motion/react";
import { BookOpen, Check } from "lucide-react";

/**
 * SchoolBooks Achievement Icon
 * 
 * Represents completion and achievement in learning.
 * Features a navy book with an orange checkmark badge.
 */
const SchoolBooksIcon = ({
    size = 64,
    primaryColor = "#2d3e50",
    accentColor = "#f97316",
}) => {
    return (
        <div
            className="relative group flex items-center justify-center"
            style={{ width: size, height: size }}
        >
            <motion.div
                initial={{ scale: 0.9, rotate: -3 }}
                animate={{ scale: 1, rotate: 0 }}
                transition={{
                    type: "spring",
                    stiffness: 150,
                    damping: 12,
                }}
                className="absolute inset-0 rounded-[1.5rem] shadow-xl flex flex-col items-center justify-center overflow-hidden"
                style={{ backgroundColor: primaryColor }}
            >
                <div className="absolute left-0 top-0 bottom-0 w-3 bg-white/5" />
                <div className="absolute left-4 top-0 bottom-0 w-[1px] bg-white/10" />
                <BookOpen
                    size={size * 0.35}
                    color="white"
                    className="opacity-30"
                />
            </motion.div>

            <motion.div
                className="absolute"
                initial={{ x: 8, y: 8, scale: 0, opacity: 0 }}
                animate={{ x: 0, y: 0, scale: 1, opacity: 1 }}
                transition={{
                    delay: 0.3,
                    type: "spring",
                    stiffness: 200,
                    damping: 15,
                }}
                style={{
                    right: "-12%",
                    top: "-12%",
                    filter:
                        "drop-shadow(0 8px 12px rgba(249, 115, 22, 0.3))",
                }}
            >
                <div
                    className="rounded-full flex items-center justify-center border-[3px] border-white shadow-sm"
                    style={{
                        backgroundColor: accentColor,
                        width: size * 0.55,
                        height: size * 0.55,
                    }}
                >
                    <Check
                        size={size * 0.32}
                        color="white"
                        strokeWidth={4}
                    />
                </div>
            </motion.div>
        </div>
    );
};

export default SchoolBooksIcon;
