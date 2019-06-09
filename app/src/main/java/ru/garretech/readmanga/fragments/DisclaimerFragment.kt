package ru.garretech.readmanga.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment

import ru.garretech.readmanga.R


class DisclaimerFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_disclaimer, container, false)
        val button = view.findViewById<Button>(R.id.dissmissDisclaimerButton)

        button.setOnClickListener { dismiss() }
        return view
    }
}
